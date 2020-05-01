package goldimax.tesseract

import com.elbekD.bot.types.Message
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessage
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.net.URL
import javax.imageio.ImageIO

fun extractRichMessage(content: String): List<Element> =
    Jsoup.parse(content, "", Parser.xmlParser()).select("title").toList()

val logger: Logger = LogManager.getLogger("forward")
val forward: (UniBot) -> Unit = { uniBot ->
    val handleQQ: suspend GroupMessage.(String) -> Unit = lambda@{
        if (drive) return@lambda
        val tGroup = uniBot.connections.findTGByQQ(subject.id)
        if (tGroup == null) {
            logger.info("cannot find connect by tg ${subject.id}")
            return@lambda
        }

        fun getNick(qq: Member) = when {
            qq.nameCard.isNotEmpty() -> qq.nameCard
            qq.nick.isNotEmpty() -> qq.nick
            else -> qq.id.toString()
        }

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
                is ForwardMessage -> msg.nodeList.joinTo(
                    msgStringBuilder.append("[Forward]\n"),
                    "\n"
                ) { "${it.senderName}: ${it.message.contentToString()}" }
                is FlashImage -> {
                    if (forwardFlash) {
                        uniBot.tg.sendPhoto(tGroup, msg.image.url(), "${getNick(sender)}: [闪照]")
                    } else msgStringBuilder.append("[闪照]")
                }
                is RichMessage -> {
                    // TODO: process XML "聊天记录"
                    msgStringBuilder.append(extractRichMessage(msg.content))
                }
            }
            if (msgStringBuilder.isNotEmpty()) uniBot.tg.sendMessage(tGroup, "${getNick(sender)}: $msgStringBuilder")
        }
    }

    val handleTg: suspend (Message) -> Unit = lambda@{ msg ->
        val qq = uniBot.connections.findQQByTG(msg.chat.id)
        if (qq == null) {
            logger.info("cannot find connect by qq ${msg.chat.id}")
            return@lambda
        }
        val qGroup = uniBot.qq.groups[qq]

        fun getNick(msg: Message): String {
            return msg.from?.let { from ->
                "${from.first_name} ${from.last_name.orEmpty()}: "
            }.orEmpty()
        }

        logger.debug("forward tg $msg")
        val nick = getNick(msg).toMessage()
        msg.text?.let { text ->
            val cap = msg.reply_to_message?.let { rMsg ->
                val rNick = getNick(rMsg)
                "[Reply👆${rNick}]".toMessage()
            } ?: "".toMessage()
            qGroup.sendMessage(cap.plus(nick + text))
        }

        // Usually, it hold a thumbnail and a original image, get the original image(the bigger one)
        msg.photo?.maxBy { it.file_size }?.let {
            val image = ImageIO.read(URL(uniBot.tgFileUrl(it.file_id)).openStream())
            qGroup.sendMessage(nick + qGroup.uploadImage(image))
        }
        msg.sticker?.let {
            val filepath = uniBot.tgFileUrl(it.file_id)
            if (filepath.endsWith(".tgs")) {
                // TODO: Support .tgs format animated sticker
                qGroup.sendMessage(nick + " Unsupported .tgs format animated sticker")
            } else {
                val image = ImageIO.read(URL(filepath).openStream())
                qGroup.sendMessage(nick + qGroup.uploadImage(image))
            }
        }
        msg.animation?.let {
            val image = ImageIO.read(URL(uniBot.tgFileUrl(it.file_id)).openStream())
            qGroup.sendMessage(nick + qGroup.uploadImage(image))
        }
    }

    uniBot.tg.onMessage(handleTg)
    uniBot.qq.subscribeGroupMessages { contains("", onEvent = handleQQ) }
}

var drive = false
var forwardFlash = true
val manager = { uniBot: UniBot ->
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
    }

    uniBot.tg.onCommand("/drive") { msg, _ ->
        drive = true
        uniBot.tg.sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
    }
    uniBot.tg.onCommand("/park") { msg, _ ->
        drive = false
        uniBot.tg.sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
    }
}
