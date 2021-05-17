package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.apache.log4j.LogManager.getLogger
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
            checkNotNull(cmd) { "Enter 'next', 'prev' or page num." }
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

    private val json: Map<String, List<Map<String, String>>>? =
        getJson("core", "key", "picture", "json")

    private val dic = json?.let{ it.map { (k, v) -> k.toLong() to v.map {
        (it["name"] ?: error("wrong config, pic no 'name'")) to
                (it["uuid"] ?: error("wrong config, pic no 'uuid'"))
    }.toMap().toMutableMap() }.toMap().toMutableMap() } ?: mutableMapOf()

    private val logger = getLogger("Picture")

    private fun save() {
        putJson("core", "key", "picture", "json",
            JsonObject(dic.mapKeys { it.key.toString() }
                .mapValues { JsonArray(it.value.map {
                    JsonObject(mapOf("name" to it.key, "uuid" to it.value)) }) }))
    }

    private fun handleRemove(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            testSu()

            val picName = message.plainText().removePrefix("plz forget").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            dic[source.group.id]?.let { dic ->
                checkNotNull(dic[picName]) { "Cannot find picture called $picName" }
                ImageMgr.remove(UUID.fromString(dic[picName]))
                dic.remove(picName)
                save()
            } ?: reply("Cannot find picture called $picName")
            quoteReply("Done.")
        }
    }

    private fun handleSearch(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = message.plainText()
                .removePrefix("look up").trim().toRegex()
            val list = mutableListOf<String>()
            dic[source.group.id]?.let { dic ->
                list.addAll(dic.keys
                    .filter { picName in it })
            }
            dic[Connections.findTGByQQ(source.group.id)]?.let { dic ->
                list.addAll(dic.keys.filter { picName in it })
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
            val maybe = dic[source.group.id]?.filter { it.key.contains(reg) }?.values?.randomOrNull()
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
            checkNull(dic[source.group.id]
                ?.get(picName)) { "There is already a picture called $picName." }
            val picPath = UUID.randomUUID()
            val pic = message[Image]
            checkNotNull(pic) { "Cannot find picture in your message." }
            ImageMgr[picPath] = pic.bypes()
            if (dic[source.group.id] == null)
                dic[source.group.id] = mutableMapOf(picName to picPath.toString())
            else dic[source.group.id]!![picName] = picPath.toString()
            save()
            quoteReply("Done.")
        }
    }

    private fun handleImReq(): suspend GroupMessageEvent.(String) -> Unit = {
        val picName = message.plainText().trim()
        val maybe = dic[source.group.id]?.get(picName)
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
            startsWith("", onEvent = handleImReq())
            startsWith("uuid$", true) {
                error {
                    quoteReply(dic[source.group.id]!![it]!!)
                }
            }
        }

        with(UniBot.tg) {
            onCommand("/say") { msg, picName ->
                error(msg) {
                    check(!picName.isNullOrBlank()) { "Pardon?" }
                    val reg = picName.trim().toRegex()
                    val list = mutableListOf<String>()
                    list.addAll(dic[msg.chat.id]
                        ?.filter { it.key.contains(reg) } ?.values ?: emptyList())
                    list.addAll(dic[Connections.findQQByTG(msg.chat.id)]
                        ?.filter { it.key.contains(reg) } ?.values ?: emptyList())
                    val uuid = list.randomOrNull()
                    checkNotNull(uuid) { "Cannot find picture called $picName." }
                    val temp = File(uuid)
                    temp.createNewFile()
                    temp.writeBytes(ImageMgr[UUID.fromString(uuid)]!!)
                    sendPhoto(msg.chat.id, temp).whenComplete { t, _ ->
                        val qq = Connections.findQQByTG(msg.chat.id)
                        if (qq != null) GlobalScope.launch {
                            val qGroup = UniBot.qq.getGroup(qq)
                            if (qGroup != null) History.insert(qGroup.sendImage(temp).source, t)
                            temp.delete()
                        } else temp.delete()
                    }
                }
            }

            UniBot.tgListener.add {
                if (it.text.isNullOrBlank()) return@add
                val maybe = dic[it.chat.id]?.get(it.text?.trim())
                    ?: dic[Connections.findQQByTG(it.chat.id)]?.get(it.text?.trim())
                if (maybe.isNullOrBlank()) return@add
                val temp = File(maybe)
                temp.createNewFile()
                temp.writeBytes(ImageMgr[UUID.fromString(maybe)]!!)
                sendPhoto(it.chat.id, temp).whenComplete { t, _ ->
                    val qq = Connections.findQQByTG(it.chat.id)
                    if (qq != null) GlobalScope.launch {
                        val qGroup = UniBot.qq.getGroup(qq)
                        if (qGroup != null) History.insert(qGroup.sendImage(temp).source, t)
                        temp.delete()
                    } else temp.delete()
                }
            }

            onCommand("/lookup") { msg, search ->
                logger.debug("lookup with $msg")
                error(msg) {
                    val list = mutableListOf<String>()
                    dic[msg.chat.id]?.let { dic ->
                        list.addAll(search?.run{
                            dic.keys.filter { toRegex() in it }
                        } ?: dic.keys)
                    }
                    dic[Connections.findQQByTG(msg.chat.id)]?.let { dic ->
                        list.addAll(search?.run {
                            dic.keys.filter { toRegex() in it }
                        } ?: dic.keys)
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
                    val uuid = dic[msg.chat.id]?.get(picName)
                    checkNotNull(uuid) { "Cannot find picture called $picName." }

                    ImageMgr.remove(UUID.fromString(uuid))
                    dic[msg.chat.id]!!.remove(picName)
                    save()
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
                    checkNull(dic[msg.chat.id]?.get(picName))
                        { "There is already a picture called $picName." }
                    val picID = UUID.randomUUID()
                    val pic = msg.photo?.maxByOrNull { it.file_size }?.let {
                        URL(tgFileUrl(it.file_id)).openStream()
                    }?.readAllBytes()
                    checkNotNull(pic) { "Cannot find picture in your message." }
                    ImageMgr[picID] = pic
                    if (dic[msg.chat.id] == null)
                        dic[msg.chat.id] = mutableMapOf(picName to picID.toString())
                    else dic[msg.chat.id]!![picName] = picID.toString()
                    save()
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }
    }
}
