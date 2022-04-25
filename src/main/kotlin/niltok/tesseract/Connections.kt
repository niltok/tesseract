@file:Suppress("JoinDeclarationAndAssignment")

package niltok.tesseract

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.event.subscribeGroupMessages

object Connections {
    val connect: MutableList<Connection> =
        UniBot.redis.connect().sync().get("core:connections")?.let { Json.decodeFromString(it) } ?: mutableListOf()
    fun findQQByTG(tg: Long): Long? =
        connect.firstOrNull() {
            if (it.enable) {
                when (it) {
                    is Connection.GroupForward -> it.groups.any { it is IMGroup.TG && it.id == tg }
                    else -> false
                }
            } else false
        }?.let { when (it) {
            is Connection.GroupForward -> it.groups.firstNotNullOfOrNull { if (it is IMGroup.QQ) it.id else null }
            else -> null
        } }
    fun findTGByQQ(qq: Long): Long? =
        connect.firstOrNull() {
            if (it.enable) {
                when (it) {
                    is Connection.GroupForward -> it.groups.any { it is IMGroup.QQ && it.id == qq }
                    else -> false
                }
            } else false
        }?.let { when (it) {
            is Connection.GroupForward -> it.groups.firstNotNullOfOrNull { if (it is IMGroup.TG) it.id else null }
            else -> null
        } }
    fun save() {
        UniBot.redis.connect().sync().set("core:connections", Json.encodeToString(connect))
    }
    init {
        save()

        with(UniBot.tg) {
            onCommand("/disconnect_all") { msg, _ ->
                error(msg) {
                    testSu(msg)

                    connect.removeIf { cg ->
                        when (cg) {
                            is Connection.GroupForward -> {
                                cg.groups.removeIf { it is IMGroup.TG && it.id == msg.chat.id }
                                cg.groups.isEmpty()
                            }
                            is Connection.SingleForward ->
                                cg.to is IMGroup.TG && cg.to.id == msg.chat.id
                                        || cg.from is IMGroup.TG && cg.from.id == msg.chat.id
                        }
                    }
                    save()

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
            onCommand("/connect") { msg, cmd ->
                error(msg) {
                    testSu(msg)

                    connect.add(Connection.GroupForward("", true,
                        mutableListOf(IMGroup.TG(msg.chat.id), IMGroup.QQ(cmd!!.toLong()))))
                    save()

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }

        UniBot.qq.eventChannel.subscribeGroupMessages {
            case("plz disconnect all") {
                error {
                    testSu()

                    connect.removeIf { cg ->
                        when (cg) {
                            is Connection.GroupForward -> {
                                cg.groups.removeIf { it is IMGroup.QQ && it.id == group.id }
                                cg.groups.isEmpty()
                            }
                            is Connection.SingleForward ->
                                cg.to is IMGroup.QQ && cg.to.id == group.id
                                        || cg.from is IMGroup.QQ && cg.from.id == group.id
                        }
                    }
                    save()

                    reply("Done.")
                }
            }
        }
    }
}
