package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import com.luciad.imageio.webp.WebPReadParam
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessage
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import java.net.URL
import javax.imageio.ImageIO

class Forward(private val uniBot: UniBot) {

    private var drive = false
    private var forwardFlash = true

    data class Connection(val qq: Long, val tg: Long)

    private val connect = uniBot.conf.array<JsonObject>("connect")!!
        .map { Connection(it.long("qq")!!, it.long("tg")!!) }

    private fun handleQQ(): suspend GroupMessage.(String) -> Unit = lambda@{
        if (drive) return@lambda

        val connect = connect.find { x -> x.qq == subject.id }

        fun getNick(qq: Member) = when {
            qq.nameCard.isNotEmpty() -> qq.nameCard
            qq.nick.isNotEmpty() -> qq.nick
            else -> qq.id.toString()
        }

        if (connect != null) {
            val tGroup = connect.tg
            message.forEach {
                val msg = StringBuilder()
                when (it) {
                    is PlainText -> msg.append(it)
                    is Image ->
                        uniBot.tg.sendPhoto(tGroup, it.url(), "${getNick(sender)}: ")
                    is At -> msg.append(it.display).append(" ")
                    is AtAll -> msg.append(AtAll.display).append(" ")
                    is QuoteReply -> msg.append("[Reply👆")
                        .append(getNick(subject.members[it.source.fromId]))
                        .append(": ")
                        .append(it.source.originalMessage.contentToString())
                        .append("]")
                    is Face -> msg.append(it.contentToString())
                    is ForwardMessage ->
                        msg.append("[Forward]\n").append(it.nodeList.joinToString("\n") {
                            "${it.senderName}: ${it.message.contentToString()}"
                        })
                    is FlashImage -> {
                        if (forwardFlash) {
                            uniBot.tg.sendPhoto(tGroup, it.image.url(), "${getNick(sender)}: [闪照]")
                        } else msg.append("[闪照]")
                    }
                    is RichMessage -> msg.append("{").append(it.content).append("}")
                }
                if (msg.isNotEmpty()) uniBot.tg.sendMessage(tGroup, "${getNick(sender)}: $msg")
            }
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
        msg.photo?.let {
            it.forEach {
                val image = ImageIO.read(URL(filePath(it.file_id)).openStream())
                nick.plus(qGroup.uploadImage(image).also { println(it.imageId) })
            }
            qGroup.sendMessage(nick)
        }
        msg.sticker?.let {
            val image = ImageIO.read(URL(filePath(it.file_id)).openStream())
            qGroup.sendMessage(nick + qGroup.uploadImage(image))
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