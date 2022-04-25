package niltok.tesseract

import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isAdministrator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageSerializers
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.concurrent.timerTask
import net.mamoe.mirai.message.data.Message as QMsg

val SJson = Json {
    serializersModule = Json.serializersModule + MessageSerializers.serializersModule
    ignoreUnknownKeys = true
}

suspend fun renderTgs(tgs: InputStream): ByteArray {
    val gzip = withContext(Dispatchers.IO) {
        GZIPInputStream(tgs)
    }
    val script = InputStreamReader(gzip).readText()
    return WebPage.renderLottie(script)
}

fun RedisClient.connectRaw(): StatefulRedisConnection<String, ByteArray> =
    connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE))!!

fun db(): RedisCommands<String, String> = UniBot.redisClient.sync()
fun dbRaw(): RedisCommands<String, ByteArray> = UniBot.redisClientRaw.sync()

fun Group.toIMGroup() = IMGroup.QQ(id)

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
    subject.sendMessage(msg)

suspend fun MessageEvent.reply(msg: String) =
    reply(msg.toPlainText())

suspend fun MessageEvent.quoteReply(msg: QMsg) =
    subject.sendMessage(QuoteReply(source) + msg)

suspend fun MessageEvent.quoteReply(msg: String) =
    quoteReply(msg.toPlainText())

// qq
suspend inline fun MessageEvent.error(after: () -> Unit): Unit {
    try {
        after()
    } catch (e: Exception) {
        quoteReply(e.localizedMessage)
    }
}

suspend fun <A, T> A.doIO(io: A.() -> T): T = withContext(Dispatchers.IO) { io() }

suspend inline fun GroupMessageEvent.error(after: () -> Unit): Unit {
    try {
        after()
    } catch (e: Exception) {
        quoteReply(e.localizedMessage)
    }
}

fun MessageEvent.testGSu() =
    check(SUManager.isSuperuser(IMUser.QQ(sender.id))) { "Sorry, you are not superuser." }

fun MessageEvent.testSu() =
    check(SUManager.isSuperuser(IMUser.QQ(sender.id))
            || this is GroupMessageEvent && sender.isAdministrator()) { "Sorry, you are not superuser or admin." }

fun testSu(msg: Message) =
    check(SUManager.isSuperuser(IMUser.TG(msg.from!!.id.toLong()))) { "Sorry, you are not superuser." }

fun Member.displayName() =
    when {
        nameCard.isNotEmpty() -> nameCard
        nick.isNotEmpty() -> nick
        else -> id.toString()
    }

suspend fun Image.bytes(): ByteArray = this.let {
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

inline fun Bot.error(id: Long, from: Long?, after: () -> Unit) = try {
    after()
} catch (e: Exception) {
    sendMessage(id, e.localizedMessage, replyTo = from)
}

inline fun Bot.error(msg: Message, after: () -> Unit) =
    error(msg.chat.id, msg.message_id) { after() }

infix fun IMGroup.transfer(g: IMGroup) = FireWall.transferable(this, g)

fun IMGroup.allConnection(): List<IMGroup> =
    listOf(this) + connection()

fun IMGroup.connection(): List<IMGroup> =
    Connections.connect.flatMap { conn ->
        when {
            conn.enable && conn is Connection.GroupForward && conn.groups.contains(this) ->
                conn.groups.filter { it != this } .toSet()
            conn.enable && conn is Connection.SingleForward && conn.from == this ->
                setOf(conn.to)
            else -> setOf()
        }
    }.toList()

fun IMMember.group() = when (this) {
    is IMMember.QQ -> IMGroup.QQ(group)
    is IMMember.TG -> IMGroup.TG(group)
}

fun IMMember.user() = when (this) {
    is IMMember.QQ -> IMUser.QQ(id)
    is IMMember.TG -> IMUser.TG(id)
}

fun Member.toIMMember() = IMMember.QQ(group.id, id)

infix fun MessageSource.eq(ms: MessageSource) =
    ids.contentEquals(ms.ids) &&
            fromId == ms.fromId &&
            targetId == ms.targetId

fun List<UniMsgType>.text() = joinToString { when (it) {
    is UniMsgType.Text -> it.text
    else -> ""
} }

fun List<UniMsgType>.content() = joinToString { when (it) {
    is UniMsgType.Text -> it.text
    is UniMsgType.Image -> " [图片] "
    is UniMsgType.Mention -> it.user.toString()
    is UniMsgType.MentionAll -> " [@全体] "
} }

fun UniMsgType.Image.get() = ImageMgr[id]

suspend fun Contact.send(msg: List<UniMsgType>): List<IMMsgRef> =
    listOf(IMMsgRef.QQ(sendMessage(msg.map { m ->
        when (m) {
            is UniMsgType.Text -> PlainText(m.text)
            is UniMsgType.Image -> ImageMgr[m.id]?.inputStream()?.uploadAsImage(this) as SingleMessage?
                ?: PlainText("[Broken Image]") as SingleMessage
            is UniMsgType.Mention ->
                if (m.user is IMUser.QQ) At(m.user.id)
                else PlainText("[@${SJson.encodeToString(m.user)}]")
            is UniMsgType.MentionAll -> AtAll
        }
    }.toMessageChain()).source))

//suspend fun Chat.send(msg: List<UniMsgType>): List<IMMsgRef> {
//    val content = msg.content()
//    val images = msg.filterIsInstance<UniMsgType.Image>()
//    if (images.isEmpty()) {
//        val ref = UniBot.tgc.sendMessage(this, content)
//        return listOf(IMMsgRef.TG(this.id.chatId, null, ref.messageId))
//    }
//    val ref = UniBot.tgc.sendMediaGroup(this, )
//}

fun fixRateTimer(start: Date, duration: Long, task: () -> Unit): Timer {
    var s = start.toInstant()
    while (s < Instant.now()) s += Duration.ofMillis(duration)
    val timer = Timer()
    timer.scheduleAtFixedRate(timerTask { task() }, Date.from(s), duration)
    return timer
}
