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

object Forward {
    private val logger: Logger = LogManager.getLogger(this.javaClass)
    val forward: () -> Unit = {
        val handleQQ: suspend GroupMessageEvent.(String) -> Unit = lambda@{
            val tGroup = Connections.findTGByQQ(subject.id)
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
                                reply = History.getTG(msg.source)
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
                0 -> UniBot.tg.sendMessage(tGroup, caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, u -> logger.info(u); History.insert(source, t.message_id) }
                1 -> UniBot.tg.sendPhoto(tGroup, imgs.first(), caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, _ -> History.insert(source, t.message_id) }
                else -> UniBot.tg.sendMediaGroup(tGroup, imgs.map {
                        UniBot.tg.mediaPhoto(it, caption =  caption, parseMode = "html") }, replyTo = reply)
                    .whenComplete{ t, _ -> History.insert(source, t.first().message_id) }
            }
        }

        val handleTg: suspend (TGMsg) -> Unit = lambda@{ msg ->
            logger.debug("receive tg ${msg.text}")
            // if (drive) return@lambda
            val qq = Connections.findQQByTG(msg.chat.id)
            logger.info("transfering to ${msg.chat.id}")
            if (qq == null) {
                return@lambda
            }
            val qGroup = UniBot.qq.groups[qq]


            val msgs = mutableListOf<Message>(PlainText((msg.displayName() + msg.forward_from.let {
                it?.let { "[Forwarded from ${it.first_name} ${it.last_name.orEmpty()}]" } ?: ""
            } + ": ")))

            msg.reply_to_message?.let {
                val id = History.getQQ(it.message_id)
                if (id == null) msgs.add(PlainText("[Reply👆${it.displayName()}]"))
                else msgs.add(QuoteReply(id))
            }

            msg.text?.let {
                msgs.add(PlainText(it))
            }

            msg.caption?.let {
                msgs.add(PlainText(it))
            }

            // Usually, it hold a thumbnail and a original image, get the original image(the bigger one)
            msg.photo?.maxByOrNull { it.file_size }?.let {
                val image = ImageIO.read(URL(tgFileUrl(it.file_id)).openStream())
                msgs.add(qGroup.uploadImage(image))
            }

            msg.sticker?.let {
                val filePath = tgFileUrl(it.file_id)
                if (filePath.endsWith(".tgs")) {
                    // TODO: Support .tgs format animated sticker
                    msgs.add(PlainText(" Unsupported .tgs format animated sticker"))
                } else {
                    val image = ImageIO.read(URL(filePath).openStream())
                    msgs.add(qGroup.uploadImage(image))
                }
            }

            msg.animation?.let {
                val image = ImageIO.read(URL(tgFileUrl(it.file_id)).openStream())
                msgs.add(qGroup.uploadImage(image))
            }

            val qid = qGroup.sendMessage(msgs.asMessageChain()).source
            History.insert(qid, msg.message_id)

            Unit
        }

        UniBot.tgListener.add(handleTg)
        UniBot.tg.onEditedMessage(handleTg)
        UniBot.qq.subscribeGroupMessages { contains("", onEvent = handleQQ) }
    }

    val manager = {
        UniBot.qq.subscribeGroupMessages {

            startsWith("QQIMG", true) {
                quoteReply(Image(it.trim()))
            }

            startsWith("FACE", true) {
                quoteReply(Face(it.trim().toInt()))
            }
        }

        UniBot.tg.run {
            onCommand("/drive") { msg, _ ->
                // drive = true
                // sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
            onCommand("/park") { msg, _ ->
                // drive = false
                // sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
            onCommand("/is_drive") { msg, _ ->
                // sendMessage(msg.chat.id, drive.toString())
            }
        }
    }

    init {
        manager()
        forward()
    }
}
