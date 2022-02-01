package niltok.tesseract

import kotlinx.serialization.encodeToString

object FireWall {
    fun transferable(a: IMGroup, b: IMGroup): Boolean = when {
        b is IMGroup.QQ && db().sismember("core:drive:list", SJson.encodeToString(a)) -> false
        else -> true
    }
    init {
        with(UniBot.tg) {
            onCommand("/drive") { msg, _ ->
                error(msg) {
                    db().sadd("core:drive:list", SJson.encodeToString(IMGroup.TG(msg.chat.id) as IMGroup))
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
            onCommand("/park") { msg, _ ->
                error(msg) {
                    db().srem("core:drive:list", SJson.encodeToString(IMGroup.TG(msg.chat.id) as IMGroup))
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }
    }
}