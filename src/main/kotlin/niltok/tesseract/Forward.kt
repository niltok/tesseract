package niltok.tesseract

import com.elbekD.bot.Bot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
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
            logger.info("transferring to $tGroup")

            var reply: Long? = null
            val imgs = mutableListOf<String>()
            val files = mutableListOf<String>()
            suspend fun parseMsg(message: MessageChain): String {
                val msgText = StringBuilder()
                message.forEach { msgText.append(
                    when (it) {
                        is FlashImage -> {
                            imgs.add(it.image.queryUrl())
                            ""
                        }
                        is Image -> {
                            imgs.add(it.queryUrl())
                            ""
                        }
                        is PlainText -> it.content
                        is At -> it.getDisplay(group) + " "
                        is AtAll -> AtAll.display + " "
                        is QuoteReply -> {
                            reply = History.getTG(it.source)
                            if (reply == null)
                                String.format(
                                    "[Reply\uD83D\uDC46%s: %s]",
                                    subject.members[it.source.fromId]?.displayName(),
                                    it.source.originalMessage.contentToString()
                                )
                            else ""
                        }
                        is Face -> it.contentToString()
                        is ForwardMessage ->
                            "[Forward] {\n ${ // TODO: msg chain
                                it.nodeList.joinToString("\n") { "  ${it.senderName}: ${
                                    runBlocking { parseMsg(it.messageChain) }
                                }" }
                            } }"
                        is FileMessage -> {
                            (it.takeIf { it.size <= 20 * 1024 * 1024 } ?: return "[Large File ${it.name}]")
                                .toAbsoluteFile(group)?.getUrl()?.let { url ->
                                    files.add(url)
                                    "[File ${it.name}]"
                                } ?: "[Broken File ${it.name}]"
                        }
                        is RichMessage -> {
                            val json = SJson.parseToJsonElement(it.content).jsonObject
                            json["meta"]?.jsonObject?.values?.map { it.jsonObject }
                                ?.joinToString("\n") {
                                    "<a href='${
                                        it["qqdocurl"]?.jsonPrimitive?.contentOrNull
                                    }'>${
                                        it["desc"]?.jsonPrimitive?.contentOrNull
                                    }</a>"
                                } ?: ""
                        }
                        else -> it.contentToString()
                    })
                }
                return msgText.toString()
            }

            val caption = String.format("<b>%s</b>: %s", sender.displayName(), parseMsg(message))
            when {
                imgs.isEmpty() && files.isEmpty() ->
                    UniBot.tg.sendMessage(tGroup, caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, _ -> History.insert(source, t) }
                imgs.size == 1 && files.isEmpty() ->
                    UniBot.tg.sendPhoto(tGroup, imgs.first(), caption, replyTo = reply, parseMode = "html")
                    .whenComplete { t, _ -> History.insert(source, t) }
                imgs.isEmpty() && files.size == 1 ->
                    UniBot.tg.sendDocument(tGroup, files.first(), caption = caption, replyTo = reply, parseMode = "html")
                        .whenComplete { t, _ -> History.insert(source, t) }
                else -> UniBot.tg.sendMediaGroup(tGroup, imgs.map {
                    UniBot.tg.mediaPhoto(it, caption =  caption, parseMode = "html")
                } + files.map {
                    UniBot.tg.mediaDocument(it, caption = caption, parseMode = "html")
                }, replyTo = reply)
                    .whenComplete { t, _ -> History.insert(source, t.first()) }
            }
        }

        val handleTg: suspend Bot.(TGMsg) -> Unit = lambda@{ msg ->
            if (Instant.ofEpochSecond(msg.date.toLong()) + Duration.ofMinutes(3) < Date().toInstant())
                return@lambda
            logger.debug("receive tg: ${msg.text}")
            val qq = Connections.findQQByTG(msg.chat.id)
            if (qq == null || (msg.text?.contains("#NSFQ") == true)
                || msg.text?.contains("#SFQ") != true && !(IMGroup.TG(msg.chat.id) transfer IMGroup.QQ(qq))
            ) {
                return@lambda
            }
            val qGroup = UniBot.qq.groups[qq] ?: return@lambda
            logger.info("transferring to ${qGroup.id}")

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

            msg.document?.let {
                if (it.file_size > 20 * 1024 * 1024) {
                    msgs.add(PlainText("[Large File ${it.file_name}]"))
                    return@let
                }
                val file = URL(tgFileUrl(it.file_id)).openStream().toExternalResource().toAutoCloseable()
                msgs.add(qGroup.files.uploadNewFile("/${msg.displayName()}-${it.file_name}", file).toMessage())
            }

            msg.sticker?.let {
                val filePath = tgFileUrl(it.file_id)
                if (filePath.endsWith(".tgs")) {
                    val image = ByteArrayInputStream(renderTgs(URL(filePath).openStream())).uploadAsImage(qGroup)
                    msgs.add(image)
                } else {
                    val output = ByteArrayOutputStream()
                    ImageIO.write(ImageIO.read(URL(filePath).openStream()), "png", output)
                    val image = ByteArrayInputStream(output.toByteArray()).uploadAsImage(qGroup)
                    msgs.add(image)
                }
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
