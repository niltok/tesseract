package niltok.tesseract

import com.elbekD.bot.types.Message
import net.mamoe.mirai.message.data.MessageSource
import java.time.Duration
import java.util.*

object History {
    data class TGID(val id: Long, val group: Long)

    private data class Msg(val qqID: MessageSource, val tgID: TGID, val time: Date = Date())

    private val timeTable : MutableList<Msg> = LinkedList<Msg>()

    private fun update() {
        while (timeTable.isNotEmpty() &&
            (timeTable.first().time.toInstant() + Duration.ofDays(30) < Date().toInstant() ||
            timeTable.size > 10000)) {
            timeTable.removeFirst()
        }
    }

    fun insert(qqID: MessageSource, tgID: Message) {
        update()
        timeTable.add(Msg(qqID, TGID(tgID.message_id, tgID.chat.id)))
    }

    fun getTG(id: MessageSource): Long? {
        update()
        return timeTable.firstOrNull { it.qqID eq id }?.tgID?.id
    }

    fun getQQ(id: Message): MessageSource? {
        update()
        return timeTable.firstOrNull {
            it.tgID.id == id.message_id && it.tgID.group == id.chat.id
        }?.qqID
    }
}