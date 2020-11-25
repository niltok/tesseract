package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.queryUrl
import net.mamoe.mirai.message.uploadImage
import org.apache.log4j.LogManager.getLogger
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@ExperimentalStdlibApi
object Picture {

    class PageTransaction(
        private val dic: List<String>
    ): Transaction {
        var page = 0
        override suspend fun handle(id: UUID, message: MessageEvent) {
            val cmd = message.message[PlainText]?.contentToString()?.trim()
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

    private val json: Map<String, List<Map<String, String>>> =
        getJson("core", "key", "picture", "json")

    private val dic = json.map { (k, v) -> k.toLong() to v.map {
        (it["name"] ?: error("wrong config, pic no 'name'")) to
                (it["uuid"] ?: error("wrong config, pic no 'uuid'"))
    }.toMap().toMutableMap() }.toMap().toMutableMap()

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

            val picName = (message[PlainText]?.toString() ?: "").removePrefix("plz forget").trim()
            dic[source.group.id]?.let { dic ->
                check(picName.isNotEmpty()) { "Pardon?" }
                checkNotNull(dic[picName]) { "Cannot find picture called $picName" }
                ImageMgr.remove(UUID.fromString(dic[picName]))
                dic.remove(picName)
                save()
            }
            quoteReply("Done.")
        }
    }

    private fun handleSearch(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = (message[PlainText]?.toString() ?: "").removePrefix("look up").trim().toRegex()
            dic[source.group.id]?.let { dic ->
                val fdic = dic.keys
                    .filter { picName in it }
                val names = fdic
                    .take(20)
                    .joinToString(separator = "\n")
                if (names == "") quoteReply("Empty.")
                else {
                    val id = TransactionManager.insert(PageTransaction(fdic))
                    val mr = quoteReply("$names\n#Page (0 / ${(fdic.size - 1) / 20})")
                    TransactionManager.attach(mr.source, id)
                }
            }
        }
    }

    private fun handleReq(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = (message[PlainText]?.toString() ?: "").removePrefix("say").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            val reg = picName.toRegex()
            val maybe = dic[source.group.id]?.filter { it.key.contains(reg) }?.values?.randomOrNull()
            checkNotNull(maybe) { "Cannot find picture called $picName." }
            val img = uploadImage(ImageMgr[UUID.fromString(maybe)]!!.inputStream())
            val qid = reply(img).source
            Connections.findTGByQQ(subject.id)?.let {
                UniBot.tg.sendPhoto(it, img.queryUrl())
                    .whenComplete { t, _ -> History.insert(qid, t.message_id) }
            }
        }
    }

    private fun handleAdd(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            val picName = (message[PlainText]?.toString() ?: "").removePrefix("remember").trim()
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
        val picName = (message[PlainText]?.toString() ?: "").trim()
        val maybe = dic[source.group.id]?.get(picName)
        if (maybe != null) {
            val img = uploadImage(ImageMgr[UUID.fromString(maybe)]!!.inputStream())
            val qid = reply(img).source
            Connections.findTGByQQ(subject.id)?.let {
                UniBot.tg.sendPhoto(it, img.queryUrl())
                    .whenComplete { t, _ -> History.insert(qid, t.message_id) }
            }
        }
    }

    init {
        UniBot.qq.subscribeGroupMessages {
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
                    val uuid = dic[Connections.findQQByTG(msg.chat.id)]
                        ?.filter { it.key.contains(reg) }?.values?.randomOrNull()
                    checkNotNull(uuid) { "Cannot find picture called $picName." }
                    GlobalScope.launch {
                        val gid = UniBot.qq.getGroup(
                            Connections.findQQByTG(msg.chat.id) ?: return@launch)
                        val img = gid.uploadImage(ImageMgr[UUID.fromString(uuid)]!!.inputStream())
                        val qid = gid.sendMessage(img).source
                        sendPhoto(msg.chat.id, img.queryUrl()).whenComplete { t, _ ->
                            History.insert(qid, t.message_id)
                        }
                    }
                }
            }

            UniBot.tgListener.add {
                if (it.text.isNullOrBlank()) return@add
                val maybe = dic[Connections.findQQByTG(it.chat.id)]?.get(it.text!!.trim())
                if (maybe.isNullOrBlank()) return@add
                GlobalScope.launch {
                    val gid = UniBot.qq.getGroup(Connections.findQQByTG(it.chat.id) ?: return@launch)
                    val img = gid.uploadImage(ImageMgr[UUID.fromString(maybe)]!!.inputStream())
                    val qid = gid.sendMessage(img).source
                    sendPhoto(it.chat.id, img.queryUrl()).whenComplete { t, _ ->
                        History.insert(qid, t.message_id)
                    }
                }
            }

            onCommand("/lookup") { msg, search ->
                logger.debug("lookup with $msg")
                error(msg) {
                    val result = (dic[Connections.findQQByTG(msg.chat.id)]
                        ?.let { dic ->
                            (search?.run {
                                dic.keys.filter { toRegex() in it }
                            } ?: dic.keys)
                        } ?: emptyList()).take(20)
                        .joinToString("\n")
                        .or("Empty.")
                    sendMessage(msg.chat.id, result, replyTo = msg.message_id)
                }
            }

            onCommand("/forget") { msg, picName ->
                logger.debug("forget $picName with $msg")
                error(msg) {
                    testSu(msg)
                    check(!picName.isNullOrBlank()) { "Pardon?" }
                    val uuid = dic[Connections.findQQByTG(msg.chat.id)]?.get(picName)
                    checkNotNull(uuid) { "Cannot find picture called $picName." }

                    ImageMgr.remove(UUID.fromString(uuid))
                    dic[Connections.findQQByTG(msg.chat.id)]!!.remove(picName)
                    save()
                    sendMessage(msg.chat.id, "Done", replyTo = msg.message_id)
                }
            }
        }
    }
}
