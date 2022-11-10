package niltok.tesseract

import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FireWall {
    fun transferable(a: IMGroup, b: IMGroup): Boolean = when {
        a is IMGroup.TG && b is IMGroup.QQ -> !(transaction {
            ConnectInfo.select { ConnectInfo.tg eq a.id }.firstOrNull()?.get(ConnectInfo.drive) } ?: false)
        else -> true
    }
    init {
        with(UniBot.tg) {
            onCommand("/drive") { msg, _ ->
                error(msg) {
                    transaction {
                        ConnectInfo.update({ ConnectInfo.tg eq msg.chat.id }) {
                            it[drive] = true
                        }
                    }
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
            onCommand("/park") { msg, _ ->
                error(msg) {
                    transaction {
                        ConnectInfo.update({ ConnectInfo.tg eq msg.chat.id }) {
                            it[drive] = false
                        }
                    }
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }
    }
}