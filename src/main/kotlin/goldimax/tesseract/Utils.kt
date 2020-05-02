package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.ContactMessage
import java.io.File

fun getJson(fileName: String): JsonObject =
    Parser.default()
        .parse(fileName) as JsonObject

fun putJson(fileName: String, obj: JsonObject) =
    File(fileName).writeText(obj.toJsonString(true))

fun String.or(string: String) = if (isNullOrBlank()) string else this

// qq
suspend inline fun ContactMessage.error(after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    reply(e.localizedMessage)
}

fun ContactMessage.testSu(bot: UniBot) =
    check(bot.suMgr.isSuperuser(QQUser(sender.id))) { "Sorry, you are not superuser." }

fun Member.displayName() =
    when {
        nameCard.isNotEmpty() -> nameCard
        nick.isNotEmpty() -> nick
        else -> id.toString()
    }


suspend fun UniBot.tgFileUrl(fileID: String) =
    "https://api.telegram.org/file/bot${tgToken}/${
    tg.getFile(fileID).await().file_path}"

// tg
fun Message.displayName() =
    from?.run {
        "$first_name ${last_name.orEmpty()}: "
    }.orEmpty()

inline fun Bot.error(id: Long, from: Int?, after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    sendMessage(id, e.localizedMessage, replyTo = from)
}

inline fun Bot.error(msg: Message, after: () -> Unit) =
    error(msg.chat.id, msg.message_id) { after() }