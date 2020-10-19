package goldimax.tesseract

import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.*
import java.time.Duration
import java.util.*

interface Transaction {
    suspend fun handle(id: UUID, message: MessageEvent)
}

@ExperimentalStdlibApi
class TransactionManager(uniBot: UniBot) {
    data class ActionData(val id: UUID, val time: Date, val action: Transaction)
    private val transactions = mutableListOf<ActionData>()

    data class AttachData(val id: UUID, val time: Date, val ms: MessageSource)
    private val attached = mutableListOf<AttachData>()

    private fun update() {
        while (transactions.isNotEmpty() &&
            transactions.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            transactions.removeFirst()
        }
        while (attached.isNotEmpty() &&
            attached.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            attached.removeFirst()
        }
    }

    fun insert(t: Transaction): UUID {
        val id = UUID.randomUUID()!!
        transactions.add(ActionData(id, Date(), t))
        return id
    }

    fun remove(id: UUID) {
        transactions.removeIf { it.id == id }
    }

    fun attach(ms: MessageSource, id: UUID) {
        attached.add(AttachData(id, Date(), ms))
    }

    init {
        uniBot.qq.subscribeMessages {
            startsWith("") {
                update()
                val at = message[QuoteReply] ?: return@startsWith
                val id = attached.firstOrNull { it.ms eq at.source }?.id ?: return@startsWith
                val ta = transactions.firstOrNull { it.id == id }?.action
                if (ta == null) quoteReply("Transaction expired.")
                else ta.handle(id, this)
            }
        }
    }
}