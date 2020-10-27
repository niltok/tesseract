package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.queryUrl
import net.mamoe.mirai.message.sendImage
import net.mamoe.mirai.message.uploadImage
import org.apache.log4j.LogManager.getLogger
import java.io.File
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@ExperimentalStdlibApi
class Picture(private val uniBot: UniBot) {

    private val json: Map<String, List<Map<String, String>>> =
        uniBot.getJson("core", "key", "picture", "json")

    private val dic = json.map { (k, v) -> k.toLong() to v.map {
        it["name"]!! to it["uuid"]!!
    }.toMap().toMutableMap() }.toMap().toMutableMap()

    private val logger = getLogger("Picture")

    private fun save() {
        uniBot.putJson("core", "key", "picture", "json",
            JsonObject(dic.mapKeys { it.key.toString() }
                .mapValues { JsonArray(it.value.map { JsonObject(mapOf("name" to it.key, "uuid" to it.value)) }) }))
    }

    private fun handleRemove(): suspend GroupMessageEvent.(String) -> Unit = {
        error {
            testSu(uniBot)

            val picName = (message[PlainText]?.toString() ?: "").removePrefix("plz forget").trim()
            dic[source.group.id]?.let { dic ->
                check(picName.isNotEmpty()) { "Pardon?" }
                checkNotNull(dic[picName]) { "Cannot find picture called $picName" }
                uniBot.imageMgr.remove(UUID.fromString(dic[picName]))
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
                quoteReply(dic.keys.filter { picName in it } .take(20)
                    .joinToString(separator = "\n").or("Empty."))
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
            val img = uploadImage(uniBot.imageMgr[UUID.fromString(maybe)]!!.inputStream())
            val qid = reply(img).source
            uniBot.connections.findTGByQQ(subject.id)?.let {
                uniBot.tg.sendPhoto(it, img.queryUrl())
                    .whenComplete { t, _ -> uniBot.history.insert(qid, t.message_id) }
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
            uniBot.imageMgr[picPath] = pic.bypes()
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
            val img = uploadImage(uniBot.imageMgr[UUID.fromString(maybe)]!!.inputStream())
            val qid = reply(img).source
            uniBot.connections.findTGByQQ(subject.id)?.let {
                uniBot.tg.sendPhoto(it, img.queryUrl())
                    .whenComplete { t, _ -> uniBot.history.insert(qid, t.message_id) }
            }
        }
    }

    init {
        uniBot.qq.subscribeGroupMessages {
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

        with(uniBot.tg) {
            onCommand("/say") { msg, picName ->
                error(msg) {
                    check(!picName.isNullOrBlank()) { "Pardon?" }
                    val reg = picName.trim().toRegex()
                    val uuid = dic[uniBot.connections.findQQByTG(msg.chat.id)]
                        ?.filter { it.key.contains(reg) }?.values?.randomOrNull()
                    checkNotNull(uuid) { "Cannot find picture called $picName." }
                    GlobalScope.launch {
                        val gid = uniBot.qq.getGroup(
                            uniBot.connections.findQQByTG(msg.chat.id) ?: return@launch)
                        val img = gid.uploadImage(uniBot.imageMgr[UUID.fromString(uuid)]!!.inputStream())
                        val qid = gid.sendMessage(img).source
                        sendPhoto(msg.chat.id, img.queryUrl()).whenComplete { t, _ ->
                            uniBot.history.insert(qid, t.message_id)
                        }
                    }
                }
            }

            uniBot.tgListener.add {
                if (it.text.isNullOrBlank()) return@add
                val maybe = dic[uniBot.connections.findQQByTG(it.chat.id)]?.get(it.text!!.trim())
                if (maybe.isNullOrBlank()) return@add
                GlobalScope.launch {
                    val gid = uniBot.qq.getGroup(uniBot.connections.findQQByTG(it.chat.id) ?: return@launch)
                    val img = gid.uploadImage(uniBot.imageMgr[UUID.fromString(maybe)]!!.inputStream())
                    val qid = gid.sendMessage(img).source
                    sendPhoto(it.chat.id, img.queryUrl()).whenComplete { t, _ ->
                        uniBot.history.insert(qid, t.message_id)
                    }
                }
            }

            onCommand("/lookup") { msg, search ->
                logger.debug("lookup with $msg")
                error(msg) {
                    val result = (dic[uniBot.connections.findQQByTG(msg.chat.id)]
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
                    testSu(uniBot, msg)
                    check(!picName.isNullOrBlank()) { "Pardon?" }
                    val uuid = dic[uniBot.connections.findQQByTG(msg.chat.id)]?.get(picName)
                    checkNotNull(uuid) { "Cannot find picture called $picName." }

                    uniBot.imageMgr.remove(UUID.fromString(uuid))
                    dic[uniBot.connections.findQQByTG(msg.chat.id)]!!.remove(picName)
                    save()
                    sendMessage(msg.chat.id, "Done", replyTo = msg.message_id)
                }
            }
        }
    }
}
