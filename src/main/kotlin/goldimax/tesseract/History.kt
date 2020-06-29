package goldimax.tesseract

import net.mamoe.mirai.message.data.MessageSource
import java.time.Duration
import java.util.*

@ExperimentalStdlibApi
class History {
    private data class Msg(val qqID: MessageSource, val tgID: Int, val time: Date = Date())

    private val fromQQ = mutableMapOf<MessageSource, Int>()
    private val fromTG = mutableMapOf<Int, MessageSource>()
    private val timeTable = mutableListOf<Msg>()

    fun update() {
        while (timeTable.isNotEmpty() && timeTable.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            timeTable.first().let {
                fromQQ.remove(it.qqID)
                fromTG.remove(it.tgID)
            }
            timeTable.removeFirst()
        }
    }

    fun insert(qqID: MessageSource, tgID: Int) {
        update()
        fromQQ[qqID] = tgID
        fromTG[tgID] = qqID
        timeTable.add(Msg(qqID, tgID))
    }

    fun getTG(id: MessageSource): Int? {
        update()
        return fromQQ[id]
    }

    fun getQQ(id: Int): MessageSource? {
        update()
        return fromTG[id]
    }
}