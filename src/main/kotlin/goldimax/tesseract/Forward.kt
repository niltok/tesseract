package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import net.mamoe.mirai.event.events.GroupMessageEvent
import com.elbekD.bot.types.Message as TGMsg
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder
import java.net.URL
import javax.imageio.ImageIO

object Forward {
    private val logger: Logger = LogManager.getLogger(this.javaClass)

    private val drive: MutableMap<Long, Boolean> =
        UniBot.table.read("core", listOf("key" to "forward"))
            ?.get("drive")?.asString()?.let { Klaxon().parse<Map<String, Boolean>>(it)
                ?.mapKeys { (k, _) -> k.toLong() } ?. toMutableMap() }
            ?: mutableMapOf()

    private fun save() {
        UniBot.table.write("core", listOf("key" to "forward"),
            listOf("drive" to cVal(JsonObject(drive.mapKeys { (k, _) -> k.toString() }).toJsonString())))
    }

    val forward: () -> Unit = {
        val handleQQ: suspend GroupMessageEvent.(String) -> Unit = lambda@{
            val tGroup = Connections.findTGByQQ(subject.id)
            if (tGroup == null) {
                logger.info("cannot find connect by qq ${subject.id}")
                return@lambda
            }

            var reply: Int? = null
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
                                            msg.nodeList.map { "  ${it.senderName}: ${it.messageChain}" } 
                                                .joinToString("\n")
                                        } }"
                                    is RichMessage -> {
                                        val json = Parser.default().parse(msg.content.byteInputStream()) as JsonObject
                                        json.obj("meta")?.values?.map { it as JsonObject }?.joinToString("\n") {
                                            "<a href='${it.string("qqdocurl")}'>${it.string("desc")}</a>"
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

        val handleTg: suspend (TGMsg) -> Unit = lambda@{ msg ->
            logger.debug("receive tg ${msg.text}")
            if (drive[msg.chat.id] == true) return@lambda
            val qq = Connections.findQQByTG(msg.chat.id)
            logger.info("transfering to ${msg.chat.id}")
            if (qq == null) {
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

            val qid = qGroup.sendMessage(msgs.toMessageChain()).source
            History.insert(qid, msg)

            Unit
        }

        UniBot.tgListener.add(handleTg)
        UniBot.tg.onEditedMessage(handleTg)
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

        UniBot.tg.run {
            onCommand("/drive") { msg, _ ->
                drive[msg.chat.id] = true
                save()
                sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
            onCommand("/park") { msg, _ ->
                drive[msg.chat.id] = false
                save()
                sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
            onCommand("/is_drive") { msg, _ ->
                sendMessage(msg.chat.id, drive[msg.chat.id].toString(), replyTo = msg.message_id)
            }
        }
    }

    init {
        manager()
        forward()
    }
}
