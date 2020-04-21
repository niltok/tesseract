package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.ContactMessage
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import java.io.File
import java.util.*

class Picture(
    private val bot: UniBot,
    private val name: String
) {
    init {
        File("$name.json").run {
            if (!exists()) {
                createNewFile()
                //language=JSON
                writeText("""{"ids": []}""")
            }
        }
        File(name).run { if (!exists()) mkdir() }
    }

    private val json = getJson("$name.json")

    val dic = json.array<JsonObject>("ids")!!.map { x ->
        x.string("name")!! to x.string("uuid")!!
    }.toMap().toMutableMap()

    fun save() {
        json["ids"] = JsonArray(dic.map { (a, b) ->
            JsonObject(mapOf("name" to a, "uuid" to b))
        })
        putJson(name, json)
    }

    init {
        bot.qq.subscribeMessages {
            startsWith("remember", onEvent = handleAdd())
            startsWith("say", onEvent = handleReq())
            startsWith("look up", onEvent = handleSearch())
        }
    }

    private fun handleSearch(): suspend ContactMessage.(String) -> Unit = {
        error {
            val picName = message[PlainText].toString().removePrefix("look up").trim().toRegex()
            quoteReply(dic.keys.filter { picName in it }.joinToString(separator = "\n"))
        }
    }

    private fun handleReq(): suspend ContactMessage.(String) -> Unit = {
        error {
            val picName = message[PlainText].toString().removePrefix("say").trim()
            check(picName.isNotEmpty()) { "Pardon?" }
            val maybe = dic[picName]
            checkNotNull(maybe) { "Cannot find picture called $picName." }
            reply(uploadImage(File("pic/$maybe")))
        }
    }

    private fun handleAdd(): suspend ContactMessage.(String) -> Unit = {
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
}