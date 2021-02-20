package goldimax.tesseract

import com.alicloud.openservices.tablestore.SyncClient
import com.alicloud.openservices.tablestore.model.*
import com.beust.klaxon.*
import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.Message as QMsg
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.io.File
import java.lang.StringBuilder
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.concurrent.timerTask

inline fun <reified T> getJson(
    table: String, key: String, keyName: String, column: String): T =
    UniBot.table.read(table, listOf(key to keyName))!!.get(column)!!.asString()
        .let { Klaxon().parse(it)!! }

inline fun <reified T> getJson_(
    table: String, key: String, keyName: String, column: String): T =
    UniBot.table.read(table, listOf(key to keyName))!!.get(column)!!.asString()
        .let { Parser.default().parse(StringBuilder(it)) as T }

fun putJson(table: String, key: String, keyName: String, column: String, obj: JsonBase) =
    UniBot.table.write(table, listOf(key to keyName), listOf(column to cVal(obj.toJsonString())))

fun String.or(string: String) = if (isNullOrBlank()) string else this

fun <T> checkNull(x: T?, msg: () -> Any) =
    check(x == null, msg)

fun QMsg.plainText() = this.toMessageChain().filterIsInstance<PlainText>().joinToString { it.content }

suspend fun GroupMessageEvent.reply(msg: QMsg) =
     group.sendMessage(msg)

suspend fun GroupMessageEvent.reply(msg: String) =
    group.sendMessage(msg)

suspend fun GroupMessageEvent.quoteReply(msg: QMsg) =
    group.sendMessage(QuoteReply(source) + msg)

suspend fun GroupMessageEvent.quoteReply(msg: String) =
    quoteReply(msg.toPlainText())

suspend fun MessageEvent.reply(msg: QMsg) =
    if (this is GroupMessageEvent) group.sendMessage(msg)
    else sender.sendMessage(msg)

suspend fun MessageEvent.reply(msg: String) =
    reply(msg.toPlainText())

suspend fun MessageEvent.quoteReply(msg: QMsg) =
    if (this is GroupMessageEvent) group.sendMessage(QuoteReply(source) + msg)
    else sender.sendMessage(QuoteReply(source) + msg)

suspend fun MessageEvent.quoteReply(msg: String) =
    quoteReply(msg.toPlainText())

// qq
suspend inline fun MessageEvent.error(after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    reply(e.localizedMessage)
}

suspend inline fun GroupMessageEvent.error(after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    reply(e.localizedMessage)
}

fun MessageEvent.testSu() =
    check(SUManager.isSuperuser(QQUser(sender.id))) { "Sorry, you are not superuser." }

fun testSu(msg: Message) =
    check(SUManager.isSuperuser(TGUser(msg.from!!.id.toLong()))) { "Sorry, you are not superuser." }

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

suspend fun Image.bypes() = this.let {
    withContext(Dispatchers.IO) { URL(it.queryUrl()).openStream().readAllBytes() }
}

suspend fun tgFileUrl(fileID: String) =
    "https://api.telegram.org/file/bot${UniBot.tgToken}/${
    UniBot.tg.getFile(fileID).await().file_path}"

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

fun SyncClient.remove(table: String, key: List<Pair<String, String>>) {
    val change = RowDeleteChange(table, PrimaryKey(key.map {
        PrimaryKeyColumn(it.first, PrimaryKeyValue.fromString(it.second)) }))
    deleteRow(DeleteRowRequest(change))
}

fun cVal(v: String)    = ColumnValue.fromString(v)
fun cVal(v: Long)      = ColumnValue.fromLong(v)
fun cVal(v: ByteArray) = ColumnValue.fromBinary(v)

operator fun Row.get(key: String) = getColumn(key).firstOrNull() ?. value

infix fun MessageSource.eq(ms: MessageSource) =
    ids.contentEquals(ms.ids) &&
            fromId == ms.fromId &&
            targetId == ms.targetId

suspend fun SingleMessage.toJson() = when (this) {
    is At -> JsonObject(mapOf("type" to "at", "value" to target))
    is Face -> JsonObject(mapOf("type" to "face", "value" to id))
    is Image -> JsonObject(mapOf("type" to "image",
        "value" to ImageMgr.new(bypes())))
    is PlainText -> JsonObject(mapOf("type" to "text", "value" to content))
    else -> JsonObject(mapOf("type" to "unknown"))
}

suspend fun List<SingleMessage>.toJson() =
    JsonArray(this.map { it.toJson() })

suspend fun Group.jsonMessage(json: JsonObject) = when (json.string("type")!!) {
    "at" -> this[json.long("value")!!]?.let { At(it) } ?: "@${json.long("value")}".toPlainText()
    "face" -> Face(json.int("value")!!)
    "image" -> ImageMgr.get(UUID.fromString(json.string("value")!!))!!.inputStream().uploadAsImage(this)
    "text" -> PlainText(json.string("value")!!)
    else -> PlainText("")
}

suspend fun Group.jsonMessage(json: JsonArray<JsonObject>) =
    json.map { jsonMessage(it) } .toMessageChain()

fun fixRateTimer(start: Date, duration: Long, task: () -> Unit): Timer {
    var s = start.toInstant()
    while (s < Instant.now()) s += Duration.ofMillis(duration)
    val timer = Timer()
    timer.scheduleAtFixedRate(timerTask { task() }, Date.from(s), duration)
    return timer
}

suspend fun PipelineContext<Unit, ApplicationCall>.error(after: suspend () -> Unit) {
    try {
        after()
    } catch (e: Exception) {
        call.respondText(e.localizedMessage, status = HttpStatusCode.NotFound)
    }
}