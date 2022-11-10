package niltok.tesseract

import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.ImageType
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiLogger
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.net.URL
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object Picture {

    class PageQQTransaction(
        private val dic: List<String>
    ): QQTransaction {
        var page = 0
        override suspend fun handle(id: UUID, message: MessageEvent) {
            val cmd = message.message.plainText().trim()
            when (cmd) {
                "next" -> {
                    check(page < (dic.size - 1) / 20) { "Out of upper bound." }
                    page++
                }
                "prev" -> {
                    check(page > 0) { "Out of lower bound." }
                    page--
                }
                else -> {
                    val p = cmd.toIntOrNull()
                    checkNotNull(p) { "Not 'next', 'prev' or page num." }
                    check(0 <= p && p <= (dic.size - 1) / 20) { "Out of range." }
                    page = p
                }
            }
            val names = dic
                .drop(20 * page)
                .take(20)
                .joinToString(separator = "\n")
            val mr = message.quoteReply("$names\n#Page ($page / ${(dic.size - 1) / 20})")
            TransactionManager.attach(mr.source, id)
        }
    }

    class PageTGKeys(private val dic: List<String>): TGKeys {
        var page = 0
        override fun handle(id: UUID, msg: String, message: Message) {
            when(msg) {
                "n" -> {
                    if (page < (dic.size - 1) / 20) page++
                }
                "p" -> {
                    if (page > 0) page--
                }
            }
            val keys = mutableListOf<InlineKeyboardButton>()
            if (page > 0) keys.add(InlineKeyboardButton("Prev",
                callback_data = "p"))
            if (page < (dic.size - 1) / 20) keys.add(InlineKeyboardButton("Next",
                callback_data = "n"))
            UniBot.tg.editMessageText(message.chat.id, message.message_id,
                text = dic.drop(page * 20).take(20).joinToString("\n") +
                        "\n#Page($page / ${(dic.size - 1) / 20})",
                markup = InlineKeyboardMarkup(listOf(keys)))
        }
    }

    private fun getDic(g: IMGroup): MutableMap<String, String> =
        transaction {
            PicIndex.select { PicIndex.group eq SJson.encodeToString(g) }.firstOrNull()?.get(PicIndex.pics)
        }?.let {
            SJson.decodeFromString(it)
        } ?: mutableMapOf()

    private fun setDic(g: IMGroup, m: MutableMap<String, String>) {
        transaction {
            PicIndex.replace {
                it[group] = SJson.encodeToString(g)
                it[pics] = SJson.encodeToString(m)
            }
        }
    }

    private fun updateDic(g: IMGroup, f: (MutableMap<String, String>) -> Unit) {
        val m = getDic(g)
        f(m)
        setDic(g, m)
    }

    private val logger = MiraiLogger.Factory.create(this::class)

    private fun handleRemove(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            testSu()

            val picName = message.plainText().removePrefix("plz forget").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            updateDic(group.toIMGroup()) { dic ->
                checkNotNull(dic[picName]) { "Cannot find picture called $picName" }
                ImageMgr.remove(UUID.fromString(dic[picName]))
                dic.remove(picName)
            }
            quoteReply("Done.")
        }
    }

    private fun handleSearch(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = message.plainText()
                .removePrefix("look up").trim().toRegex()
            val list = group.toIMGroup().allConnection().flatMap { g ->
                getDic(g).keys.filter { picName in it }
            }
            val names = list
                .take(20)
                .joinToString(separator = "\n")
            if (names == "") quoteReply("Empty.")
            else {
                val id = TransactionManager.insert(PageQQTransaction(list))
                val mr = quoteReply("$names\n#Page (0 / ${(list.size - 1) / 20})")
                TransactionManager.attach(mr.source, id)
            }
        }
    }

    private fun handleReq(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = message.plainText().removePrefix("say").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            val reg = picName.toRegex()
            val maybe = group.toIMGroup().allConnection().flatMap {
                getDic(it).mapNotNull { (u, v) -> if (reg in u) v else null }
            }.randomOrNull()
            checkNotNull(maybe) { "Cannot find picture called $picName." }
            val img = ImageMgr[UUID.fromString(maybe)]!!.inputStream().uploadAsImage(group)
            val qid = reply(img).source
            Connections.findTGByQQ(subject.id)?.let {
                UniBot.tg.sendPhoto(it, img.queryUrl())
                    .whenComplete { t, _ -> History.insert(qid, t) }
            }
        }
    }

    private fun handleAdd(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = message.plainText().removePrefix("remember").trim()
            check(picName.isNotEmpty()) { "How would you call this picture? Please try again." }
            checkNull(getDic(group.toIMGroup())[picName]) { "There is already a picture called $picName." }
            val pic = message[Image]
            checkNotNull(pic) { "Cannot find picture in your message." }
            check(pic.imageType != ImageType.UNKNOWN) { "Fetch picture failed." }
            check(pic.size < 2 * 1024 * 1024) { "Picture is larger then 2M. Too big to store." }
            val picPath = ImageMgr.new(pic.bytes())
            updateDic(group.toIMGroup()) {
                it[picName] = picPath.toString()
            }
            quoteReply("Done.")
        }
    }

    private fun handleImReq(): suspend GroupMessageEvent.(String) -> Unit = {
        val picName = message.plainText().trim()
        val maybe = group.toIMGroup().allConnection().mapNotNull { getDic(it)[picName] }.randomOrNull()
        if (maybe != null) {
            val img = ImageMgr[UUID.fromString(maybe)]!!.inputStream().uploadAsImage(group)
            val qid = reply(img).source
            Connections.findTGByQQ(subject.id)?.let {
                UniBot.tg.sendPhoto(it, img.queryUrl())
                    .whenComplete { t, _ -> History.insert(qid, t) }
            }
        }
    }

    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            startsWith("remember", onEvent = handleAdd())
            startsWith("say", onEvent = handleReq())
            startsWith("look up", onEvent = handleSearch())
            startsWith("plz forget", onEvent = handleRemove())
            startsWith("get pic uuid ", true) {
                error {
                    group.toIMGroup().allConnection()
                        .mapNotNull { g -> getDic(g)[it.trim()] }
                        .joinToString("\n")
                        .or("Not found.")
                }
            }
            always(onEvent = handleImReq())
        }

        with(UniBot.tg) {
            onCommand("/say") { msg, picName ->
                error(msg) {
                    check(!picName.isNullOrBlank()) { "Pardon?" }
                    val reg = picName.trim().toRegex()
                    val uuid = IMGroup.TG(msg.chat.id).allConnection().flatMap {
                        getDic(it).mapNotNull { (u, v) -> if (reg in u) v else null }
                    }.randomOrNull()
                    checkNotNull(uuid) { "Cannot find picture called $picName." }
                    val temp = File(uuid)
                    temp.createNewFile()
                    temp.writeBytes(ImageMgr[UUID.fromString(uuid)]!!)
                    sendPhoto(msg.chat.id, temp).whenComplete { t, _ ->
                        val qq = Connections.findQQByTG(msg.chat.id)
                        if (qq != null) runBlocking {
                            val qGroup = UniBot.qq.getGroup(qq)
                            if (qGroup != null) History.insert(qGroup.sendImage(temp).source, t)
                            temp.delete()
                        } else temp.delete()
                    }
                }
            }

            UniBot.tgListener.add {
                if (it.text.isNullOrBlank()) return@add
                val maybe = IMGroup.TG(it.chat.id).allConnection().firstNotNullOfOrNull { g ->
                    getDic(g)[it.text]
                }
                if (maybe.isNullOrBlank()) return@add
                val temp = File(maybe)
                doIO { temp.createNewFile() }
                temp.writeBytes(ImageMgr[UUID.fromString(maybe)]!!)
                sendPhoto(it.chat.id, temp).whenComplete { t, _ ->
                    val qq = Connections.findQQByTG(it.chat.id)
                    if (qq != null) runBlocking {
                        val qGroup = UniBot.qq.getGroup(qq)
                        if (qGroup != null) History.insert(qGroup.sendImage(temp).source, t)
                        temp.delete()
                    } else temp.delete()
                }
            }

            onCommand("/lookup") { msg, search ->
                logger.debug("lookup with $msg")
                error(msg) {
                    val reg = search.orEmpty().trim().toRegex()
                    val list = IMGroup.TG(msg.chat.id).allConnection().flatMap { g ->
                        getDic(g).keys.filter { reg in it }
                    }
                    if (list.size <= 20) {
                        val result = list.joinToString("\n").or("Empty.")
                        sendMessage(msg.chat.id, result, replyTo = msg.message_id)
                        return@onCommand
                    }
                    val result = list.take(20)
                        .joinToString("\n") + "\n#Page(0 / ${list.size / 20})"
                    val id = TransactionManager.insert(PageTGKeys(list))
                    sendMessage(msg.chat.id, result, replyTo = msg.message_id,
                        markup = InlineKeyboardMarkup(listOf(listOf(
                            InlineKeyboardButton("Next",
                                callback_data = "n"))))).whenComplete { t, _ ->
                        TransactionManager.attachK(t.message_id, id)
                    }
                }
            }

            onCommand("/forget") { msg, picName ->
                logger.debug("forget $picName with $msg")
                error(msg) {
                    testSu(msg)
                    check(!picName.isNullOrBlank()) { "Pardon?" }
                    val uuid = getDic(IMGroup.TG(msg.chat.id))[picName]
                    checkNotNull(uuid) { "Cannot find picture called $picName." }

                    ImageMgr.remove(UUID.fromString(uuid))
                    updateDic(IMGroup.TG(msg.chat.id)) {
                        it.remove(picName)
                    }
                    sendMessage(msg.chat.id, "Done", replyTo = msg.message_id)
                }
            }

            UniBot.tgListener.add { msg ->
                if (msg.caption == null) return@add
                if (!msg.caption!!.startsWith("remember")) return@add
                val picName = msg.caption!!.removePrefix("remember").trim()
                error(msg) {
                    check(msg.chat.id < 0) { "Unsupported for non-group chat." }
                    check(picName.isNotEmpty()) { "How would you call this picture? Please try again." }
                    checkNull(getDic(IMGroup.TG(msg.chat.id))[picName])
                        { "There is already a picture called $picName." }
                    val pic = msg.photo?.maxByOrNull { it.file_size }?.let {
                        URL(tgFileUrl(it.file_id)).openStream()
                    }?.readBytes()
                    checkNotNull(pic) { "Cannot find picture in your message." }
                    val picID = ImageMgr.new(pic)
                    updateDic(IMGroup.TG(msg.chat.id)) {
                        it[picName] = picID.toString()
                    }
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }
    }
}
