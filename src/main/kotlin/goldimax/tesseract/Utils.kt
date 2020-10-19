package goldimax.tesseract

import com.alicloud.openservices.tablestore.SyncClient
import com.alicloud.openservices.tablestore.model.*
import com.beust.klaxon.JsonBase
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.isAboutTemp
import net.mamoe.mirai.message.data.queryUrl
import java.io.File
import java.net.URL

@ExperimentalStdlibApi
inline fun <reified T> UniBot.getJson(
    table: String, key: String, keyName: String, column: String): T =
    this.table.read(table, listOf(key to keyName))!!.get(column)!!.asString()
        .let { Klaxon().parse(it)!! }

@ExperimentalStdlibApi
fun UniBot.putJson(table: String, key: String, keyName: String, column: String, obj: JsonBase) =
    this.table.write(table, listOf(key to keyName), listOf(column to cVal(obj.toJsonString())))

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

@ExperimentalStdlibApi
fun testSu(bot: UniBot, msg: Message) =
    check(bot.suMgr.isSuperuser(TGUser(msg.from!!.id.toLong()))) { "Sorry, you are not superuser." }

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
        "$first_name ${last_name.orEmpty()}"
    }.orEmpty()

inline fun Bot.error(id: Long, from: Int?, after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    sendMessage(id, e.localizedMessage, replyTo = from)
}

inline fun Bot.error(msg: Message, after: () -> Unit) =
    error(msg.chat.id, msg.message_id) { after() }

fun SyncClient.read(table: String, key: List<Pair<String, String>>): Row? {
    val query = SingleRowQueryCriteria(table, PrimaryKey(key.map{
        PrimaryKeyColumn(it.first, PrimaryKeyValue.fromString(it.second)) }))
    query.maxVersions = 1
    return getRow(GetRowRequest(query)).row
}

fun SyncClient.write(
    table: String,
    key: List<Pair<String, String>>,
    value: List<Pair<String, ColumnValue>>) {
    val change = RowUpdateChange(table, PrimaryKey(key.map {
        PrimaryKeyColumn(it.first, PrimaryKeyValue.fromString(it.second)) }))
        .put(value.map { Column(it.first, it.second) })
    updateRow(UpdateRowRequest(change))
}

fun cVal(v: String)    = ColumnValue.fromString(v)
fun cVal(v: Long)      = ColumnValue.fromLong(v)
fun cVal(v: ByteArray) = ColumnValue.fromBinary(v)

operator fun Row.get(key: String) = getColumn(key).firstOrNull() ?. value

infix fun MessageSource.eq(ms: MessageSource) =
    time == ms.time
        && id == ms.id
        && internalId == ms.internalId
        && fromId == ms.fromId
