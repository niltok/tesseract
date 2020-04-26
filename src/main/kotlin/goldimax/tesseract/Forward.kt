package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessage
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import org.apache.log4j.LogManager
import java.net.URL
import javax.imageio.ImageIO

class Forward(private val uniBot: UniBot) {

    private var drive = false
    private var forwardFlash = true

    private val logger = LogManager.getLogger(this.javaClass)

    data class Connection(val qq: Long, val tg: Long)

    private val connect = uniBot.conf.array<JsonObject>("connect")!!
        .map { Connection(it.long("qq")!!, it.long("tg")!!) }

    private fun handleQQ(): suspend GroupMessage.(String) -> Unit = lambda@{
        if (drive) return@lambda

        val connect = connect.find { x -> x.qq == subject.id } ?: return@lambda

        fun getNick(qq: Member) = when {
            qq.nameCard.isNotEmpty() -> qq.nameCard
            qq.nick.isNotEmpty() -> qq.nick
            else -> qq.id.toString()
        }

        val tGroup = connect.tg
        message.forEach { msg ->
            logger.debug("forward qq $msg")

            val msgStringBuilder = StringBuilder()
            when (msg) {
                is PlainText -> msgStringBuilder.append(msg)
                is Image ->
                    uniBot.tg.sendPhoto(tGroup, msg.url(), "${getNick(sender)}: ")
                is At -> msgStringBuilder.append(msg.display).append(" ")
                is AtAll -> msgStringBuilder.append(AtAll.display).append(" ")
                is QuoteReply -> msgStringBuilder.append("[Reply👆")
                    .append(getNick(subject.members[msg.source.fromId]))
                    .append(": ")
                    .append(msg.source.originalMessage.contentToString())
                    .append("]")
                is Face -> msgStringBuilder.append(msg.contentToString())
                is ForwardMessage ->
                    msgStringBuilder.append("[Forward]\n").append(msg.nodeList.joinToString("\n") {
                        "${it.senderName}: ${it.message.contentToString()}"
                    })
                is FlashImage -> {
                    if (forwardFlash) {
                        uniBot.tg.sendPhoto(tGroup, msg.image.url(), "${getNick(sender)}: [闪照]")
                    } else msgStringBuilder.append("[闪照]")
                }
                is RichMessage -> {
                    // TODO: process XML "聊天记录"
                    msgStringBuilder.append("{").append(msg.content).append("}")
                }
            }
            if (msgStringBuilder.isNotEmpty()) uniBot.tg.sendMessage(tGroup, "${getNick(sender)}: $msgStringBuilder")
        }
    }

    private fun handleTg(): suspend (Message) -> Unit = lambda@{ msg ->

        val connect = connect.find { x -> x.tg == msg.chat.id } ?: return@lambda
        fun getNick(msg: Message): String {
            return msg.from?.let { from ->
                "${from.first_name} ${from.last_name.orEmpty()}: "
            }.orEmpty()
        }

        val qGroup = uniBot.qq.groups[connect.qq]

        logger.debug("forward tg $msg")
        val nick = getNick(msg).toMessage()
        msg.text?.let { text ->
            val cap = msg.reply_to_message?.let { rMsg ->
                val rNick = getNick(rMsg)
                "[Reply👆${rNick}]".toMessage()
            } ?: "".toMessage()
            qGroup.sendMessage(cap.plus(nick + text))
        }

        suspend fun filePath(fileID: String) =
            "https://api.telegram.org/file/bot${uniBot.tgToken}/${
            uniBot.tg.getFile(fileID).await().file_path}"

        // Usually, it have multi PhotoSize, get the biggest
        msg.photo?.maxBy { it.file_size }?.let { it ->
            val image = ImageIO.read(URL(filePath(it.file_id)).openStream())
            qGroup.sendMessage(nick + qGroup.uploadImage(image))
        }
        msg.sticker?.let {
            val filepath = filePath(it.file_id)
            if (filepath.endsWith(".tgs")) {
                // TODO: Support .tgs format animated sticker
                qGroup.sendMessage(nick + " Unsupported .tgs format animated sticker")
            } else {
                val image = ImageIO.read(URL(filepath).openStream())
                qGroup.sendMessage(nick + qGroup.uploadImage(image))
            }
        }
        msg.animation?.let {
            val image = ImageIO.read(URL(filePath(it.file_id)).openStream())
            qGroup.sendMessage(nick + qGroup.uploadImage(image))
        }
    }

    init {
        uniBot.qq.subscribeGroupMessages {
            case("plz do not forward flash image") {
                testSu(uniBot)
                forwardFlash = false
                quoteReply("Done.")
            }
            case("plz forward flash image") {
                testSu(uniBot)
                forwardFlash = true
                quoteReply("Done.")
            }

            startsWith("QQIMG", true) {
                quoteReply(Image(it.trim()))
            }

            contains("", onEvent = handleQQ())
        }

        uniBot.tg.onMessage(handleTg())
        uniBot.tg.onCommand("/drive") { msg, _ ->
            drive = true
            uniBot.tg.sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
        }
        uniBot.tg.onCommand("/park") { msg, _ ->
            drive = false
            uniBot.tg.sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
        }
    }
}
