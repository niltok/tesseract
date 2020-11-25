package goldimax.tesseract

import com.alicloud.openservices.tablestore.SyncClient
import com.alicloud.openservices.tablestore.model.*
import com.beust.klaxon.*
import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.LowLevelAPI
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.At.Key._lowLevelConstructAtInstance
import net.mamoe.mirai.message.uploadImage
import java.io.File
import java.lang.StringBuilder
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.concurrent.timerTask

@ExperimentalStdlibApi
inline fun <reified T> getJson(
    table: String, key: String, keyName: String, column: String): T =
    UniBot.table.read(table, listOf(key to keyName))!!.get(column)!!.asString()
        .let { Klaxon().parse(it)!! }

@ExperimentalStdlibApi
inline fun <reified T> getJson_(
    table: String, key: String, keyName: String, column: String): T =
    UniBot.table.read(table, listOf(key to keyName))!!.get(column)!!.asString()
        .let { Parser.default().parse(StringBuilder(it)) as T }

@ExperimentalStdlibApi
fun putJson(table: String, key: String, keyName: String, column: String, obj: JsonBase) =
    UniBot.table.write(table, listOf(key to keyName), listOf(column to cVal(obj.toJsonString())))

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
fun MessageEvent.testSu() =
    check(SUManager.isSuperuser(QQUser(sender.id))) { "Sorry, you are not superuser." }

@ExperimentalStdlibApi
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

@ExperimentalStdlibApi
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

infix fun MessageSource.eq(ms: MessageSource) = id == ms.id
        && internalId == ms.internalId
        && fromId == ms.fromId

@ExperimentalStdlibApi
suspend fun SingleMessage.toJson() = when (this) {
    is At -> JsonObject(mapOf("type" to "at", "value" to target))
    is Face -> JsonObject(mapOf("type" to "face", "value" to id))
    is Image -> JsonObject(mapOf("type" to "image",
        "value" to ImageMgr.new(bypes())))
    is PlainText -> JsonObject(mapOf("type" to "text", "value" to content))
    else -> JsonObject(mapOf("type" to "unknown"))
}

@ExperimentalStdlibApi
suspend fun List<SingleMessage>.toJson() =
    JsonArray(this.map { it.toJson() })

@ExperimentalStdlibApi
suspend fun Group.jsonMessage(json: JsonObject) = when (json.string("type")!!) {
    "at" -> At(this[json.long("value")!!])
    "face" -> Face(json.int("value")!!)
    "image" -> uploadImage(ImageMgr.get(UUID.fromString(json.string("value")!!))!!.inputStream())
    "text" -> PlainText(json.string("value")!!)
    else -> PlainText("")
}

@ExperimentalStdlibApi
suspend fun Group.jsonMessage(json: JsonArray<JsonObject>) =
    json.map { jsonMessage(it) } .asMessageChain()

fun fixRateTimer(start: Date, duration: Long, task: () -> Unit): Timer {
    var s = start.toInstant()
    while (s < Instant.now()) s += Duration.ofMillis(duration)
    val timer = Timer()
    timer.scheduleAtFixedRate(timerTask { task() }, Date.from(s), duration)
    return timer
}