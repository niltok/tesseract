package niltok.tesseract

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import java.time.Duration
import java.time.Instant
import java.util.*

object Reminder {
    @Serializable
    data class Remind(val content: String, val duration: Long)

    private val history = mutableMapOf<Long, MutableMap<Long, Instant>>()

    private suspend fun GroupMessageEvent.setReminder(prefix: String, duration: Long) {
        error {
            val at = message.firstIsInstanceOrNull<At>()
            val id = if (at == null) {
                sender.id
            } else {
                testSu()
                at.target
            }
            val content = message.plainText().removePrefix(prefix).trim()
//            db().hset("core:reminders",
//                SJson.encodeToString(IMMember.QQ(group.id, id)),
//                SJson.encodeToString(Remind(content, duration)))
            quoteReply("Done.")
        }
    }

    init {
//        UniBot.qq.eventChannel.subscribeGroupMessages {
//            case("remove reminder") {
//                db().hdel("core:reminders", SJson.encodeToString(sender.toIMMember()))
//                quoteReply("Done.")
//            }
//            startsWith("daily reminder ") {
//                setReminder("daily reminder ", Duration.ofHours(12).toMinutes())
//            }
//            startsWith("weak reminder ") {
//                setReminder("weak reminder", Duration.ofHours(3).toMinutes())
//            }
//            startsWith("medium reminder ") {
//                setReminder("medium reminder", Duration.ofMinutes(30).toMinutes())
//            }
//            startsWith("strong reminder ") {
//                setReminder("strong reminder", Duration.ofMinutes(5).toMinutes())
//            }
//            startsWith("crazy reminder ") {
//                setReminder("crazy reminder", Duration.ofMinutes(0).toMinutes())
//            }
//            always {
//                val remind: Remind = db().hget("core:reminders",
//                    SJson.encodeToString(sender.toIMMember()))?.let {
//                    SJson.decodeFromString(it)
//                } ?: return@always
//                val his = history[source.group.id]?.get(sender.id)
//                if (his == null || Date().toInstant() > his + Duration.ofMinutes(remind.duration)) {
//                    quoteReply(remind.content)
//                    if (history[source.group.id] == null)
//                        history[source.group.id] = mutableMapOf()
//                    history[source.group.id]!![sender.id] = Date().toInstant()
//                }
//            }
//        }
    }
}