package goldimax.tesseract

import com.elbekD.bot.types.Message
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
            logger.info("cannot find connect by qq ${subject.id}")
            return@lambda
        }

        message.forEach { msg ->
            logger.debug("forward qq $msg")
            when (msg) {
                is FlashImage -> if (forwardFlash) {
                    uniBot.tg.sendPhoto(tGroup, msg.image.url(), "${sender.displayName()}: [闪照]")
                } else {
                    uniBot.tg.sendMessage(tGroup, "[闪照]")
                }
                is Image -> uniBot.tg.sendPhoto(tGroup, msg.url(), "${sender.displayName()}: ")
                else -> {
                    val msgString: String = when (msg) {
                        is PlainText -> msg.stringValue
                        is At -> msg.display + " "
                        is AtAll -> AtAll.display + " "
                        is QuoteReply -> String.format(
                            "[Reply\uD83D\uDC46%s: %s]",
                            subject.members[msg.source.fromId].displayName(),
                            msg.source.originalMessage.contentToString()
                        )
                        is Face -> msg.contentToString()
                        is ForwardMessage -> msg.nodeList.joinToString("\n", "[Forward]\n")
                        is RichMessage -> // TODO: process XML "聊天记录"
                            extractRichMessage(msg.content).map(Element::text).toString()
                        else -> "Unknown message"
                    }
                    uniBot.tg.sendMessage(tGroup, String.format("%s: %s", sender.displayName(), msgString))
                }
            }
        }
    }

    val handleTg: suspend (Message) -> Unit = lambda@{ msg ->
        val qq = uniBot.connections.findQQByTG(msg.chat.id)
        if (qq == null) {
            logger.info("cannot find connect by tg ${msg.chat.id}")
            return@lambda
        }
        val qGroup = uniBot.qq.groups[qq]

        logger.debug("forward tg $msg")
        val nick = msg.displayName().toMessage()
        msg.text?.let { text ->
            val cap = msg.reply_to_message?.let {
                "[Reply👆${it.displayName()}]"
            }.orEmpty().toMessage()
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
