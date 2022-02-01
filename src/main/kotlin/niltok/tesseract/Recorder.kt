package niltok.tesseract

import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.text.SimpleDateFormat
import java.util.*


object Recorder {
    data class Trace(
        val fileName : String,
        val tellerID : Long,
        val recordString : StringBuilder = StringBuilder()
    )
    private val record = mutableMapOf<Long, Trace>()

    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            startsWith("record start", true) {
                error {
                    check(group.id !in record.keys) { "ERROR: another record is ongoing in this group" }
                    check(it.isNotEmpty()) { "ERROR: empty record name" }
                    record[group.id] = Trace(it, sender.id)
                    quoteReply("DOMO, ${
                        record[group.id]?.tellerID?.let { group[it]?.displayName() }
                    }=san. Motor·Rainbow, recorder circuit on, 実際安い.")
                }
            }
            always {
                record[group.id]?.let { t ->
                    if (t.tellerID == sender.id) {
                        val msg = message.content.trim()
                        if (msg != "record end") {
                            t.recordString.append(msg + "\n")
                            reply("√ recording")
                        } else {
                            error {
                                t.recordString.toString().toByteArray().toExternalResource().use {
                                    group.files.uploadNewFile(
                                        "/${record[group.id]?.fileName}-${
                                            SimpleDateFormat("MM-dd-hh-mm-ss").format(Date())
                                        }.txt", it
                                    )
                                }
                            }
                            record.remove(group.id)
                        }
                    }
                }
            }
        }
    }
}