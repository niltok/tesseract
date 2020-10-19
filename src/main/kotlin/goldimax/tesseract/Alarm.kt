package goldimax.tesseract

import io.ktor.util.InternalAPI
import io.ktor.util.date.toDate
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.*
import java.time.Duration
import java.time.Year
import java.util.*
import kotlin.concurrent.timerTask

@ExperimentalStdlibApi
class AlarmTransaction(val uniBot: UniBot, val alarm: Alarm): Transaction {
    var state = 0
    var time = Calendar.getInstance()
    var duration = Duration.ofDays(1)
    @InternalAPI
    override suspend fun handle(id: UUID, message: MessageEvent) {
        when (state) {
            0 -> message.error {
                val value = message.message[PlainText]!!.content.trim()
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
                uniBot.actionMgr.attach(mr.source, id)
            }
            1 -> message.error {
                val value = message.message[PlainText]!!.content.trim()
                    .split(" ").filter { it.isNotBlank() }.map { it.trim().toLong() }
                check(value[0] > 0) { "no attack." }
                duration = Duration.ofHours(value[0]) + Duration.ofMinutes(value[1])
                state += 1
                val mr = message.quoteReply("Duration: $duration\n" +
                        "Reply this message to set alarm message")
                uniBot.actionMgr.attach(mr.source, id)
            }
            2 -> message.error {
                val msg = message.message.filter { it is PlainText || it is Image || it is Face || it is At }
                val timer = Timer()
                timer.scheduleAtFixedRate(timerTask {
                    GlobalScope.launch { message.reply(msg.asMessageChain()) }
                }, time.time, duration.toMillis())
                val timerID = UUID.randomUUID()
                val data = Alarm.AlarmData(timerID, time.time, duration.toMillis(), msg, timer)
                alarm.data.add(data)
                uniBot.actionMgr.remove(id)
                message.quoteReply("Alarm created,\n" +
                        "UUID: $timerID.")
            }
        }
    }
}

@ExperimentalStdlibApi
class Alarm(uniBot: UniBot) {
    data class AlarmData(val id: UUID, val time: Date,
                         val duration: Long, val message: List<SingleMessage>, val timer: Timer)
    val data = mutableListOf<AlarmData>()
    init {
        val alarm = this
        uniBot.qq.subscribeMessages {
            case("new alarm") {
                error {
                    val id = uniBot.actionMgr.insert(AlarmTransaction(uniBot, alarm))
                    val mr = quoteReply(
                        "[Testing, no save]Alarm creating...\n" +
                                "Reply this message to set start time,\n" +
                                "Format: offset-day hour minute"
                    )
                    uniBot.actionMgr.attach(mr.source, id)
                }
            }
            case("show alarms") {
                error {
                    quoteReply("Alarms:\n" + data.joinToString("\n") { it.id.toString() })
                }
            }
            startsWith("show alarm ", true) {
                error {
                    val alarmData = data.first { x -> x.id.toString() == it.trim() }
                    quoteReply("start: ${alarmData.time}\n" +
                            "duration: ${Duration.ofMillis(alarmData.duration)}\n" +
                            alarmData.message.joinToString { it.contentToString() })
                }
            }
        }
    }
}