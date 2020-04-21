package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import net.mamoe.mirai.message.ContactMessage
import java.io.File

fun getJson(fileName: String): JsonObject =
    Parser.default()
        .parse(fileName) as JsonObject

fun putJson(fileName: String, obj: JsonObject) =
    File(fileName).writeText(obj.toJsonString(true))

suspend fun ContactMessage.error(after: suspend () -> Unit) = try {
    after()
} catch (e: Exception) {
    reply(e.localizedMessage)
}

fun ContactMessage.testSu(bot: UniBot) =
    check(bot.isSuperuser(sender.id)) { "Sorry, you are not superuser." }
