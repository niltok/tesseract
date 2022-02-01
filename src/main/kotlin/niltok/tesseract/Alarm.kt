package niltok.tesseract

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import java.time.Duration
import java.util.*

class AlarmQQTransaction(private val group: Long): QQTransaction {
    var state = 0
    var time: Calendar = Calendar.getInstance()
    var duration: Duration = Duration.ofDays(1)
    override suspend fun handle(id: UUID, message: MessageEvent) {
        when (state) {
            0 -> message.error {
                val value = message.message.plainText().trim()
                    .split(" ").filter { it.isNotBlank() }.map { it.trim().toInt() }
                time = Calendar.getInstance()
                time.add(Calendar.DATE, value[0])
                time[Calendar.HOUR_OF_DAY] = value[1]
                time[Calendar.MINUTE] = value[2]
                time[Calendar.SECOND] = 0
                state += 1
                val mr = message.quoteReply("Alarm at ${time.time}\n" +
                        "Reply this message to set duration,\n" +
                        "Format: hour minute")
                TransactionManager.attach(mr.source, id)
            }
            1 -> message.error {
                val value = message.message.plainText().trim()
                    .split(" ").filter { it.isNotBlank() }.map { it.trim().toLong() }
                check(value[0] > 0) { "no attack." }
                duration = Duration.ofHours(value[0]) + Duration.ofMinutes(value[1])
                state += 1
                val mr = message.quoteReply("Duration: $duration\n" +
                        "Reply this message to set alarm message")
                TransactionManager.attach(mr.source, id)
            }
            2 -> message.error {
                val msg = message.message.filter {
                    it is PlainText || it is Image || it is Face ||
                            it is At && it.target != UniBot.qq.id }
                val timer = fixRateTimer(time.time, duration.toMillis()) {
                    runBlocking { message.reply(msg.toMessageChain()) }
                }
                val timerID = UUID.randomUUID()
                val data = Alarm.AlarmData(
                    timerID, time.time, duration.toMillis(),
                    group, UniMsgType.Text(""), timer
                )
                Alarm.data.add(data)
                Alarm.save()
                TransactionManager.remove(id)
                message.quoteReply("Alarm created,\n" +
                        "UUID: $timerID.")
            }
        }
    }
}

object Alarm {
    data class AlarmData(val id: UUID, val time: Date, val duration: Long,
                         val group: Long, val msg: UniMsgType, val timer: Timer)
    val data = mutableListOf<AlarmData>()
    fun save() {
    }
    init {
        save()
        UniBot.qq.eventChannel.subscribeGroupMessages {
            case("new alarm") {
                error {
                    val id = TransactionManager.insert(AlarmQQTransaction(source.group.id))
                    val mr = quoteReply(
                        "Alarm creating...\n" +
                                "Reply this message to set start time,\n" +
                                "Format: offset-day hour minute"
                    )
                    TransactionManager.attach(mr.source, id)
                }
            }
            case("show all alarms") {
                error {
                    quoteReply("Alarms:\n" + data.filter { it.group == source.group.id }
                        .joinToString("\n") { it.id.toString() })
                }
            }
            startsWith("show alarm ", true) {
                error {
                    val alarmData = data.first { x -> x.id.toString() == it.trim()
                            && x.group == source.group.id }
                    quoteReply("start: ${alarmData.time}\n" +
                            "duration: ${Duration.ofMillis(alarmData.duration)}\n")
                }
            }
            startsWith("remove alarm ", true) {
                error {
                    val alarmData = data.first { x -> x.id.toString() == it.trim()
                            && x.group == source.group.id }
                    alarmData.timer.cancel()
                    // remove Image
                    data.removeIf { it.id == alarmData.id }
                    save()
                    quoteReply("Done.")
                }
            }
        }
    }
}