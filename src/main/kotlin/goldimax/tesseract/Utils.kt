package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.queryUrl
import java.io.File
import java.net.URL

fun getJson(fileName: String): JsonObject =
    Parser.default()
        .parse(fileName) as JsonObject

fun putJson(fileName: String, obj: JsonObject) =
    File(fileName).writeText(obj.toJsonString(true))

fun String.or(string: String) = if (isNullOrBlank()) string else this

fun <T> checkNull(x: T?, msg: () -> Any) =
    check(x == null, msg)

// qq
suspend inline fun MessageEvent.error(after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    reply(e.localizedMessage)
}

@ExperimentalStdlibApi
fun MessageEvent.testSu(bot: UniBot) =
    check(bot.suMgr.isSuperuser(QQUser(sender.id))) { "Sorry, you are not superuser." }

fun Member.displayName() =
    when {
        nameCard.isNotEmpty() -> nameCard
        nick.isNotEmpty() -> nick
        else -> id.toString()
    }

suspend fun Image.downloadTo(file: File) {
    val img = this
    withContext(Dispatchers.IO) {
        URL(img.queryUrl()).openStream().transferTo(file.outputStream())
    }
}

@ExperimentalStdlibApi
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