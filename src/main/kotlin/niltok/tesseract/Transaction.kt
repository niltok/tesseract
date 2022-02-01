package niltok.tesseract

import com.elbekD.bot.types.Message
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
import java.time.Duration
import java.util.*

interface QQTransaction {
    suspend fun handle(id: UUID, message: MessageEvent)
}

interface TGTransaction {
    fun handle(id: UUID, message: Message)
}

interface TGKeys {
    fun handle(id: UUID, msg: String, message: Message)
}

object TransactionManager {
    data class QQActionData(val id: UUID, val time: Date, val action: QQTransaction)
    private val qqTransactions = mutableListOf<QQActionData>()

    data class QQAttachData(val id: UUID, val time: Date, val ms: MessageSource)
    private val qqAttached = mutableListOf<QQAttachData>()

    data class TGActionData(val id: UUID, val time: Date, val action: TGTransaction)
    private val tgTransactions = mutableListOf<TGActionData>()

    data class TGAttachData(val id: UUID, val time: Date, val msg: Long)
    private val tgAttached = mutableListOf<TGAttachData>()

    data class TGKActionData(val id: UUID, val time: Date, val action: TGKeys)
    private val tgKeys = mutableListOf<TGKActionData>()

    data class TGKAttachData(val id: UUID, val time: Date, val msg: Long)
    private val tgKAttached = mutableListOf<TGKAttachData>()

    private fun update() {
        while (qqTransactions.isNotEmpty() &&
            qqTransactions.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            qqTransactions.removeFirst()
        }
        while (qqAttached.isNotEmpty() &&
            qqAttached.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            qqAttached.removeFirst()
        }
        while (tgTransactions.isNotEmpty() &&
            tgTransactions.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            tgTransactions.removeFirst()
        }
        while (tgAttached.isNotEmpty() &&
            tgAttached.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            tgAttached.removeFirst()
        }
        while (tgKeys.isNotEmpty() &&
            tgKeys.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            tgKeys.removeFirst()
        }
        while (tgKAttached.isNotEmpty() &&
            tgKAttached.first().time.toInstant() + Duration.ofDays(1) < Date().toInstant()) {
            tgKAttached.removeFirst()
        }
    }

    fun insert(t: QQTransaction): UUID {
        val id = UUID.randomUUID()!!
        qqTransactions.add(QQActionData(id, Date(), t))
        return id
    }

    fun insert(t: TGTransaction): UUID {
        val id = UUID.randomUUID()!!
        tgTransactions.add(TGActionData(id, Date(), t))
        return id
    }

    fun insert(t: TGKeys): UUID {
        val id = UUID.randomUUID()!!
        tgKeys.add(TGKActionData(id, Date(), t))
        return id
    }

    fun remove(id: UUID) {
        qqTransactions.removeIf { it.id == id }
        qqAttached.removeIf { it.id == id }
        tgTransactions.removeIf { it.id == id }
        tgAttached.removeIf { it.id == id }
        tgKeys.removeIf { it.id == id }
        tgKAttached.removeIf { it.id == id }
    }

    fun attach(ms: MessageSource, id: UUID) {
        qqAttached.add(QQAttachData(id, Date(), ms))
    }

    fun attach(msg: Long, id: UUID) {
        tgAttached.add(TGAttachData(id, Date(), msg))
    }

    fun attachK(msg: Long, id: UUID) {
        tgKAttached.add(TGKAttachData(id, Date(), msg))
    }

    init {
        UniBot.qq.eventChannel.subscribeMessages {
            always {
                error {
                    update()
                    val at = message[QuoteReply]?.source ?: return@always
                    val id = qqAttached.firstOrNull { it.ms eq at }?.id ?: return@always
                    val ta = qqTransactions.firstOrNull { it.id == id }?.action
                    if (ta == null) quoteReply("Transaction expired.")
                    else ta.handle(id, this)
                }
            }
        }

        with(UniBot.tg) {
            UniBot.tgListener.add { msg ->
                error(msg) {
                    update()
                    val at = msg.reply_to_message?.message_id ?: return@add
                    val id = tgAttached.firstOrNull { it.msg == at }?.id ?: return@add
                    val ta = tgTransactions.firstOrNull { it.id == id }?.action
                    if (ta == null) sendMessage(msg.chat.id, "Transaction expired.")
                    else ta.handle(id, msg)
                }
            }
            onCallbackQuery {
                it.data?.let { data ->
                    it.message?.let { msg ->
                        error(msg) {
                            update()
                            val at = msg.message_id
                            val id = tgKAttached.firstOrNull { it.msg == at }?.id
                                ?: return@onCallbackQuery
                            val ta = tgKeys.firstOrNull { it.id == id }?.action
                            checkNotNull(ta) { "Transaction expired." }
                            ta.handle(id, data, msg)
                        }
                    }
                }
            }
        }

    }
}