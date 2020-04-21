package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.elbekD.bot.http.await
import com.elbekD.bot.types.Message
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessage
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import java.net.URL

class Forward(private val bot: UniBot) {

    data class Connection(val qq: Long, val tg: Long)

    private val connect = bot.conf.array<JsonObject>("connect")!!
        .map { Connection(it.long("qq")!!, it.long("tg")!!) }

    private fun handleQQ(): suspend GroupMessage.(String) -> Unit = {
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
                    is Image -> this@Forward.bot.tg.sendPhoto(tGroup, it.url(), "${getNick(sender)}: ")
                    is At -> msg.append(it.display).append(" ")
                    is AtAll -> msg.append(AtAll.display).append(" ")
                    is RichMessage -> msg.append("{").append(it.content).append("}")
                    is QuoteReply -> msg.append("[Reply👆")
                        .append(getNick(subject.members[it.source.fromId]))
                        .append(": ")
                        .append(it.source.originalMessage.contentToString())
                        .append("]")
                    is Face -> msg.append(it.contentToString())
                }
                if (msg.isNotEmpty()) this@Forward.bot.tg.sendMessage(tGroup, "${getNick(sender)}: $msg")
            }
        }
    }

    private fun handleTg(): suspend (Message) -> Unit = lambda@{ msg ->
        val connect = connect.find { x -> x.tg == msg.chat.id }
        fun getNick(msg: Message): String {
            return msg.from?.let { from ->
                "${from.first_name} ${from.last_name.orEmpty()}: "
            }.orEmpty()
        }

        if (connect == null) return@lambda
        val qGroup = bot.qq.groups[connect.qq]
        val nick = getNick(msg).toMessage()
        if (msg.text != null) {
            val cap = if (msg.reply_to_message != null) {
                val rMsg = msg.reply_to_message!!
                val rNick = getNick(rMsg)
                "[Reply👆${rNick}]".toMessage()
            } else "".toMessage()
            qGroup.sendMessage(cap.plus(nick.plus(msg.text!!)))
        }

        // TODO: Need to fix file type error
        suspend fun filePath(fileID: String) =
            "https://api.telegram.org/file/bot${bot.tgToken}/${bot.tg.getFile(fileID).await().file_path}"
        if (msg.photo != null) {
            val qMsg = nick
            msg.photo!!.forEach {
                qMsg.plus(qGroup.uploadImage(URL(filePath(it.file_id))))
            }
            qGroup.sendMessage(qMsg)
        }
        if (msg.sticker != null) {
            qGroup.sendMessage(nick.plus(qGroup.uploadImage(URL(filePath(msg.sticker!!.file_id)))))
        }
        if (msg.animation != null) {
            qGroup.sendMessage(nick.plus(qGroup.uploadImage(URL(filePath(msg.animation!!.file_id)))))
        }
    }

    init {
        bot.qq.subscribeGroupMessages {
            contains("", onEvent = handleQQ())
        }
        bot.tg.onMessage(handleTg())
    }
}