package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.ContactMessage
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import org.apache.log4j.LogManager.getLogger
import java.io.File
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

val picture: (UniBot, String) -> Unit = { uniBot: UniBot, confName: String ->
    File("$confName.json").run {
        if (!exists()) {
            createNewFile()
            //language=JSON
            writeText("""{"ids": []}""")
        }
    }
    File(confName).run { if (!exists()) mkdir() }

    val json = getJson("$confName.json")

    val dic = json.array<JsonObject>("ids")!!.map { x ->
        x.string("name")!! to x.string("uuid")!!
    }.toMap().toMutableMap()

    val logger = getLogger("Picture")

    fun save() {
        json["ids"] = JsonArray(dic.map { (a, b) ->
            JsonObject(mapOf("name" to a, "uuid" to b))
        })
        putJson("$confName.json", json)
    }

    fun handleRemove(): suspend ContactMessage.(String) -> Unit = {
        error {
            testSu(uniBot)

            val picName = message[PlainText].contentToString().removePrefix("plz forget").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            checkNotNull(dic[picName]) { "Cannot find picture called $picName" }
            dic.remove(picName)
            save()
            quoteReply("Done.")
        }
    }

    fun handleSearch(): suspend ContactMessage.(String) -> Unit = {
        error {
            val picName = message[PlainText].toString().removePrefix("look up").trim().toRegex()
            quoteReply(dic.keys.filter { picName in it }.joinToString(separator = "\n").or("Empty."))
        }
    }

    fun handleReq(): suspend ContactMessage.(String) -> Unit = {
        error {
            val picName = message[PlainText].toString().removePrefix("say").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            val maybe = dic[picName]
            checkNotNull(maybe) { "Cannot find picture called $picName." }
            reply(uploadImage(File("pic/$maybe")))
        }
    }

    fun handleAdd(): suspend ContactMessage.(String) -> Unit = {
        error {
            val picName = message[PlainText].toString().removePrefix("remember").trim()
            check(picName.isNotEmpty()) { "How would you call this picture? Please try again." }
            check(!dic.containsKey(picName)) { "There is already a picture called $picName." }
            val picPath = UUID.randomUUID()
            message[Image].downloadTo(File("pic/$picPath"))
            dic[picName] = picPath.toString()
            save()
            quoteReply("Done.")
        }
    }

    uniBot.qq.subscribeMessages {
        startsWith("remember", onEvent = handleAdd())
        startsWith("say", onEvent = handleReq())
        startsWith("look up", onEvent = handleSearch())
        startsWith("plz forget", onEvent = handleRemove())
    }

    with(uniBot.tg) {
        onCommand("/say") { msg, picName ->
            logger.debug("say $picName with $msg")
            error(msg) {
                check(!picName.isNullOrBlank()) { "Pardon?" }
                val uuid = dic[picName]
                checkNotNull(uuid) { "Cannot find picture called $picName." }
                sendPhoto(msg.chat.id, File("pic/$uuid"))
            }
        }

        onCommand("/lookup") { msg, search ->
            logger.debug("lookup with $msg")
            error(msg) {
                val result = (search?.run {
                    dic.keys.filter { toRegex() in it }
                } ?: dic.keys)
                    .joinToString("\n")
                    .or("Empty.")
                sendMessage(msg.chat.id, result, replyTo = msg.message_id)
            }
        }

        onCommand("/forget") { msg, picName ->
            logger.debug("forget $picName with $msg")
            error(msg) {
                check(!picName.isNullOrBlank()) { "Pardon?" }
                val uuid = dic[picName]
                checkNotNull(uuid) { "Cannot find picture called $picName." }

                dic.remove(picName)
                save()
                sendMessage(msg.chat.id, "Done", replyTo = msg.message_id)
            }
        }
    }
}
