package niltok.tesseract

import com.elbekD.bot.types.Message
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.OfflineMessageSource
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

object History {
    @Serializable
    data class QQID(val ids: IntArray, val internalId: IntArray, val fromId: Long, val targetId: Long)
    @Serializable
    data class TGID(val id: Long, val group: Long)

    var counter = 0

    private fun update() {
        counter++
        if (counter < 10000) return
        counter = 0
        transaction {
            Histories.deleteWhere {
                time less LocalDateTime.now() - Duration.ofDays(30)
            }
        }
    }

    fun insert(qqID: MessageSource, tgID: Message) {
        update()
        transaction {
            Histories.insert {
                it[qq] = SJson.encodeToString(QQID(qqID.ids, qqID.internalIds, qqID.fromId, qqID.targetId))
                it[tg] = SJson.encodeToString(TGID(tgID.message_id, tgID.chat.id))
                it[qqRef] = SJson.encodeToString(qqID)
                it[time] = LocalDateTime.now()
            }
        }
    }

    fun getTG(id: MessageSource): Long? {
        update()
        return transaction {
            Histories.select {
                Histories.qq eq SJson.encodeToString(QQID(id.ids, id.internalIds, id.fromId, id.targetId))
            }.firstOrNull()?.get(Histories.tg)
        }?.let { SJson.decodeFromString<TGID>(it).id }
    }

    fun getQQ(id: Message): MessageSource? {
        update()
        return transaction {
            Histories.select {
                Histories.tg eq SJson.encodeToString(TGID(id.message_id, id.chat.id))
            }.firstOrNull()?.get(Histories.qqRef)
        }?.let {
            SJson.decodeFromString(it)
        }
    }
}