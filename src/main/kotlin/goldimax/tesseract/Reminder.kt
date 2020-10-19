package goldimax.tesseract

import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.*

@ExperimentalStdlibApi
class Reminder(private val uniBot: UniBot) {
    data class Remind(val content: String, val duration: Duration)

    private val history = mutableMapOf<Long, MutableMap<Long, Instant>>()


    private val reminders: MutableMap<Long, MutableMap<Long, Remind>> = {
        val json: Map<String, Map<String, Map<String, String>>> =
            uniBot.getJson("core", "key", "reminder", "json")
        json.mapKeys { it.key.toLong() } .mapValues {
            it.value.mapKeys { it.key.toLong() }.mapValues {
                Remind(it.value["content"]!!, Duration.ofMinutes(it.value["duration"]!!.toLong()))
            }.toMutableMap()
        }.toMutableMap()
    }()

    private fun save() =
        uniBot.putJson("core", "key", "reminder", "json", JsonObject(reminders
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
            val at = message[At]
            val id = if (at == null) {
                sender.id
            } else {
                testSu(uniBot)
                at.target
            }
            if (reminders[source.group.id] == null)
                reminders[source.group.id] = mutableMapOf()
            val content = message[PlainText].toString().removePrefix(prefix).trim()
            reminders[source.group.id]!![id] = Remind(content, duration)
            save()
            quoteReply("Done.")
        }
    }

    init {
        uniBot.qq.subscribeGroupMessages {
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