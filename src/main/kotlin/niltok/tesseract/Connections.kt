@file:Suppress("JoinDeclarationAndAssignment")

package niltok.tesseract

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.event.subscribeGroupMessages
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object Connections {
    fun findQQByTG(tg: Long): Long? = transaction {
        ConnectInfo.select { ConnectInfo.tg eq tg }.firstOrNull()?.get(ConnectInfo.qq)
    }
    fun findTGByQQ(qq: Long): Long? = transaction {
        ConnectInfo.select { ConnectInfo.qq eq qq }.firstOrNull()?.get(ConnectInfo.tg)
    }

    init {
        with(UniBot.tg) {
            onCommand("/disconnect") { msg, _ ->
                error(msg) {
                    testSu(msg)

                    transaction {
                        ConnectInfo.deleteWhere { tg eq msg.chat.id }
                    }

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
            onCommand("/connect") { msg, cmd ->
                error(msg) {
                    testSu(msg)

                    transaction {
                        ConnectInfo.insert {
                            it[tg] = msg.chat.id
                            it[qq] = cmd!!.toLong()
                            it[drive] = false
                        }
                    }

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }

        UniBot.qq.eventChannel.subscribeGroupMessages {
            case("plz disconnect") {
                error {
                    testSu()

                    val id = group.id
                    transaction {
                        ConnectInfo.deleteWhere { qq eq id }
                    }

                    reply("Done.")
                }
            }
            startsWith("plz connect ") {
                error {
                    testSu()

                    val id = group.id
                    val tid = it.trim().toLong()
                    transaction {
                        ConnectInfo.insert {
                            it[qq] = id
                            it[tg] = tid
                            it[drive] = false
                        }
                    }

                    reply("Done.")
                }
            }
        }
    }
}
