package goldimax.tesseract

import com.elbekD.bot.types.Message as TGMsg
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.lang.StringBuilder
import java.net.URL
import javax.imageio.ImageIO

fun extractRichMessage(content: String): List<Element> =
    Jsoup.parse(content, "", Parser.xmlParser()).select("title").toList()

@ExperimentalStdlibApi
object Forward {
    private val logger: Logger = LogManager.getLogger(this.javaClass)
    val forward: (UniBot) -> Unit = { uniBot ->
        val handleQQ: suspend GroupMessageEvent.(String) -> Unit = lambda@{
            val tGroup = uniBot.connections.findTGByQQ(subject.id)
            if (tGroup == null) {
                logger.info("cannot find connect by qq ${subject.id}")
                return@lambda
            }

            val msgText = StringBuilder()
            var reply: Int? = null
            val imgs = mutableListOf<String>()
            message.forEach { msg ->
                logger.debug("forward qq $msg")

                when (msg) {
                    is FlashImage -> imgs.add(msg.image.url())
                    is Image -> imgs.add(msg.url())
                    else -> {
                        msgText.append(when (msg) {
                            is PlainText -> msg.content
                            is At -> msg.display + " "
                            is AtAll -> AtAll.display + " "
                            is QuoteReply -> {
                                reply = uniBot.history.getTG(msg.source)
                                if (reply == null)
                                    String.format("[Reply\uD83D\uDC46%s: %s]",
                                        subject.members[msg.source.fromId].displayName(),
                                        msg.source.originalMessage.contentToString())
                                else ""
                            }
                            is Face -> msg.contentToString()
                            is ForwardMessage -> "[Forward] {\n ${msg.nodeList.joinToString("\n")} }"
                            is RichMessage ->
                                "[XML] { ${extractRichMessage(msg.content)
                                    .joinToString("\n", transform = Element::text)} }"
                            else -> msg.contentToString()
                        })
                    }
                }
            }

            logger.info(msgText)
            logger.info(imgs)
            logger.info(reply)

            val caption = String.format("<b>%s</b>: %s", sender.displayName(), msgText)
            when (imgs.size) {
                0 -> uniBot.tg.sendMessage(tGroup, caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, u -> logger.info(u); uniBot.history.insert(source, t.message_id) }
                1 -> uniBot.tg.sendPhoto(tGroup, imgs.first(), caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, _ -> uniBot.history.insert(source, t.message_id) }
                else -> uniBot.tg.sendMediaGroup(tGroup, imgs.map {
                        uniBot.tg.mediaPhoto(it, caption =  caption, parseMode = "html") }, replyTo = reply)
                    .whenComplete{ t, _ -> uniBot.history.insert(source, t.first().message_id) }
            }
        }

        val handleTg: suspend (TGMsg) -> Unit = lambda@{ msg ->
            logger.debug("receive tg ${msg.text}")
            if (drive) return@lambda
            val qq = uniBot.connections.findQQByTG(msg.chat.id)
            logger.info("transfering to ${msg.chat.id}")
            if (qq == null) {
                return@lambda
            }
            val qGroup = uniBot.qq.groups[qq]


            val msgs = mutableListOf<Message>((msg.displayName() + ": ").toMessage())

            msg.reply_to_message?.let {
                val id = uniBot.history.getQQ(it.message_id)
                if (id == null) msgs.add("[Reply👆${it.displayName()}]".toMessage())
                else msgs.add(QuoteReply(id))
            }

            msg.text?.let {
                msgs.add(it.toMessage())
            }

            // Usually, it hold a thumbnail and a original image, get the original image(the bigger one)
            msg.photo?.maxBy { it.file_size }?.let {
                val image = ImageIO.read(URL(uniBot.tgFileUrl(it.file_id)).openStream())
                msgs.add(qGroup.uploadImage(image))
            }

            msg.sticker?.let {
                val filePath = uniBot.tgFileUrl(it.file_id)
                if (filePath.endsWith(".tgs")) {
                    // TODO: Support .tgs format animated sticker
                    msgs.add(" Unsupported .tgs format animated sticker".toMessage())
                } else {
                    val image = ImageIO.read(URL(filePath).openStream())
                    msgs.add(qGroup.uploadImage(image))
                }
            }

            msg.animation?.let {
                val image = ImageIO.read(URL(uniBot.tgFileUrl(it.file_id)).openStream())
                msgs.add(qGroup.uploadImage(image))
            }

            val qid = qGroup.sendMessage(msgs.asMessageChain()).source
            uniBot.history.insert(qid, msg.message_id)

            Unit
        }

        uniBot.tg.onMessage(handleTg)
        uniBot.qq.subscribeGroupMessages { contains("", onEvent = handleQQ) }
    }

    var drive = false
    val manager = { uniBot: UniBot ->
        uniBot.qq.subscribeGroupMessages {

            startsWith("QQIMG", true) {
                quoteReply(Image(it.trim()))
            }

            startsWith("FACE", true) {
                quoteReply(Face(it.trim().toInt()))
            }
        }

        uniBot.tg.run {
            onCommand("/drive") { msg, _ ->
                drive = true
                sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
            onCommand("/park") { msg, _ ->
                drive = false
                sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
            onCommand("/MOD") { msg, _ -> sendMessage(msg.chat.id, drive.toString())}
        }
    }

    val invoke: SubscribeType = { uniBot ->
        manager(uniBot)
        forward(uniBot)
    }
}
