package goldimax.tesseract

import net.mamoe.mirai.message.data.MessageSource
import java.time.Duration
import java.util.*

@ExperimentalStdlibApi
class History {
    private data class Msg(val qqID: MessageSource, val tgID: Int, val time: Date = Date())

    private val timeTable = mutableListOf<Msg>()

    fun update() {
        while (timeTable.isNotEmpty() &&
            timeTable.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            timeTable.removeFirst()
        }
    }

    fun insert(qqID: MessageSource, tgID: Int) {
        update()
        timeTable.add(Msg(qqID, tgID))
        println("add ${qqID.id} $tgID")
    }

    fun getTG(id: MessageSource): Int? {
        update()
        return timeTable.firstOrNull { it.qqID.time == id.time
                && it.qqID.id == id.id
                && it.qqID.internalId == id.internalId
                && it.qqID.fromId == id.fromId
        }?.tgID
    }

    fun getQQ(id: Int): MessageSource? {
        update()
        println(timeTable)
        return timeTable.firstOrNull { it.tgID == id }?.qqID
    }
}