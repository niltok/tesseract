package niltok.tesseract

import com.elbekD.bot.Bot
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiLogger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*
import javax.imageio.ImageIO
import com.elbekD.bot.types.Message as TGMsg

object Forward {
    private val logger = MiraiLogger.Factory.create(this::class)

    val forward: () -> Unit = {
        val handleQQ: suspend GroupMessageEvent.(String) -> Unit = lambda@{
            val tGroup = Connections.findTGByQQ(subject.id)
            if (tGroup == null) {
                logger.info("cannot find connect by qq ${subject.id}")
                return@lambda
            }

            var reply: Long? = null
            val imgs = mutableListOf<String>()
            suspend fun parseMsg(message: MessageChain): String {
                val msgText = StringBuilder()
                message.forEach { msg ->
                    when (msg) {
                        is FlashImage -> imgs.add(msg.image.queryUrl())
                        is Image -> imgs.add(msg.queryUrl())
                        else -> {
                            msgText.append(
                                when (msg) {
                                    is PlainText -> msg.content
                                    is At -> msg.getDisplay(group) + " "
                                    is AtAll -> AtAll.display + " "
                                    is QuoteReply -> {
                                        reply = History.getTG(msg.source)
                                        if (reply == null)
                                            String.format(
                                                "[Reply\uD83D\uDC46%s: %s]",
                                                subject.members[msg.source.fromId]?.displayName(),
                                                msg.source.originalMessage.contentToString()
                                            )
                                        else ""
                                    }
                                    is Face -> msg.contentToString()
                                    is ForwardMessage ->
                                        "[Forward] {\n ${ // TODO: msg chain
                                            msg.nodeList.joinToString("\n") { "  ${it.senderName}: ${it.messageChain}" }
                                        } }"
                                    is RichMessage -> {
                                        val json = SJson.parseToJsonElement(msg.content).jsonObject
                                        json["meta"]?.jsonObject?.values?.map { it.jsonObject }
                                            ?.joinToString("\n") {
                                                "<a href='${
                                                    it["qqdocurl"]?.jsonPrimitive?.contentOrNull
                                                }'>${
                                                    it["desc"]?.jsonPrimitive?.contentOrNull
                                                }</a>"
                                            } ?: ""
                                    }
                                    else -> msg.contentToString()
                                }
                            )
                        }
                    }
                }
                return msgText.toString()
            }

            val caption = String.format("<b>%s</b>: %s", sender.displayName(), parseMsg(message))
            when (imgs.size) {
                0 -> UniBot.tg.sendMessage(tGroup, caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, u -> logger.info(u); History.insert(source, t) }
                1 -> UniBot.tg.sendPhoto(tGroup, imgs.first(), caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, _ -> History.insert(source, t) }
                else -> UniBot.tg.sendMediaGroup(tGroup, imgs.map {
                        UniBot.tg.mediaPhoto(it, caption =  caption, parseMode = "html") }, replyTo = reply)
                    .whenComplete{ t, _ -> History.insert(source, t.first()) }
            }
        }

        val handleTg: suspend Bot.(TGMsg) -> Unit = lambda@{ msg ->
            if (Instant.ofEpochSecond(msg.date.toLong()) + Duration.ofMinutes(3) < Date().toInstant())
                return@lambda
            logger.debug("receive tg ${msg.text}")
            val qq = Connections.findQQByTG(msg.chat.id)
            logger.info("transferring to ${msg.chat.id}")
            if (qq == null || !(IMGroup.TG(msg.chat.id) transfer IMGroup.QQ(qq))) {
                return@lambda
            }
            val qGroup = UniBot.qq.groups[qq] ?: return@lambda


            val msgs = mutableListOf<Message>(PlainText((msg.displayName() + msg.forward_from.let {
                it?.let { "[Forwarded from ${it.first_name} ${it.last_name.orEmpty()}]" } ?: ""
            } + ": ")))

            msg.reply_to_message?.let {
                val id = History.getQQ(it)
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
                val image = URL(tgFileUrl(it.file_id)).openStream().uploadAsImage(qGroup)
                msgs.add(image)
            }

            msg.sticker?.let {
                val filePath = tgFileUrl(it.file_id)
                if (filePath.endsWith(".tgs")) {
                    // TODO: Support .tgs format animated sticker
                    msgs.add(PlainText(" Unsupported .tgs format animated sticker"))
                } else {
                    val output = ByteArrayOutputStream()
                    ImageIO.write(ImageIO.read(URL(filePath).openStream()), "png", output)
                    val image = ByteArrayInputStream(output.toByteArray()).uploadAsImage(qGroup)
                    msgs.add(image)
                }
            }

            msg.animation?.let {
                val image = URL(tgFileUrl(it.file_id)).openStream().uploadAsImage(qGroup)
                msgs.add(image)
            }

            msg.entities?.forEach {
                it.url?.let {
                    val s = "\n" + it
                    msgs.add(PlainText(s))
                }
            }

            val qid = qGroup.sendMessage(msgs.toMessageChain()).source
            History.insert(qid, msg)

            Unit
        }

        UniBot.tgListener.add(handleTg)
        UniBot.tg.onEditedMessage { UniBot.tg.handleTg(it) }
        UniBot.qq.eventChannel.subscribeGroupMessages { contains("", onEvent = handleQQ) }
    }

    val manager = {
        UniBot.qq.eventChannel.subscribeGroupMessages {

            startsWith("QQIMG", true) {
                quoteReply(Image(it.trim()))
            }

            startsWith("FACE", true) {
                quoteReply(Face(it.trim().toInt()))
            }
        }
    }

    init {
        manager()
        forward()
    }
}
