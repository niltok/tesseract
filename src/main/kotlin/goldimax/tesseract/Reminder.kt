package goldimax.tesseract

import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import java.time.Duration
import java.time.Instant
import java.util.*

object Reminder {
    data class Remind(val content: String, val duration: Duration)

    private val history = mutableMapOf<Long, MutableMap<Long, Instant>>()


    private val reminders: MutableMap<Long, MutableMap<Long, Remind>> = run {
        val json: Map<String, Map<String, Map<String, String>>>? =
            getJson("core", "key", "reminder", "json")
        json?.mapKeys { it.key.toLong() }?.mapValues {
            it.value.mapKeys { it.key.toLong() }.mapValues {
                Remind(it.value["content"]!!, Duration.ofMinutes(it.value["duration"]!!.toLong()))
            }.toMutableMap()
        }?.toMutableMap() ?: mutableMapOf()
    }

    private fun save() =
        putJson("core", "key", "reminder", "json", JsonObject(reminders
            .mapKeys { (k, _) -> k.toString() }
            .mapValues { (_, v) ->
                JsonObject(v.mapKeys { (k, _) -> k.toString() }
                    .mapValues { (_, v) ->
                        JsonObject(mapOf("content" to v.content, "duration" to v.duration.toMinutes().toString()))
                    })
            })
        )

    private suspend fun GroupMessageEvent.setReminder(prefix: String, duration: Duration) {
        error {
            val at = message.firstIsInstanceOrNull<At>()
            val id = if (at == null) {
                sender.id
            } else {
                testSu()
                at.target
            }
            if (reminders[source.group.id] == null)
                reminders[source.group.id] = mutableMapOf()
            val content = message.plainText().removePrefix(prefix).trim()
            reminders[source.group.id]!![id] = Remind(content, duration)
            save()
            quoteReply("Done.")
        }
    }

    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            case("remove reminder") {
                reminders[source.group.id]?.remove(sender.id)
                save()
                quoteReply("Done.")
            }
            startsWith("daily reminder ") {
                setReminder("daily reminder ", Duration.ofHours(12))
            }
            startsWith("weak reminder ") {
                setReminder("weak reminder", Duration.ofHours(3))
            }
            startsWith("medium reminder ") {
                setReminder("medium reminder", Duration.ofMinutes(30))
            }
            startsWith("strong reminder ") {
                setReminder("strong reminder", Duration.ofMinutes(5))
            }
            startsWith("crazy reminder ") {
                setReminder("crazy reminder", Duration.ofMinutes(0))
            }
            startsWith("") {
                val remind = reminders[source.group.id]?.get(sender.id) ?: return@startsWith
                val his = history[source.group.id]?.get(sender.id)
                if (his == null || Date().toInstant() > his + remind.duration) {
                    quoteReply(remind.content)
                    if (history[source.group.id] == null)
                        history[source.group.id] = mutableMapOf()
                    history[source.group.id]!![sender.id] = Date().toInstant()
                }
            }
        }
    }
}