package goldimax.tesseract

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.content
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Trace(
    var fileName: String? = null,
    var tellerID: Long = 0L,
    var tellerName: String? = null,
    var recordString: String? = null
)

object Recorder {
    private var record: HashMap<Long, Trace> = HashMap<Long, Trace>()

    private suspend fun writeTempFile(s: String): File {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile(UUID.randomUUID().toString(), "tmp")
        }
        withContext(Dispatchers.IO) { file.writeText(s) }
        return file
    }

    private suspend fun upload(group: Group, file: File) {
        withContext(Dispatchers.IO) {
            group.filesRoot.resolve(
                "/${record[group.id]?.fileName}-" +
                        SimpleDateFormat("MM月dd日-hh:mm:ss").format(Date()) +
                        ".txt"
            ).uploadAndSend(file)
        }
    }

    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            always {
                record[group.id]?.let { t ->
                    if (t.tellerID == sender.id && t.tellerName == sender.nick) {
                        val msg = message.content
                        if (msg != "/end_record") {
                            t.recordString += msg + "\n"
                            subject.sendMessage("√")
                        }
                    }
                }

                return@always
            }
            startsWith("/start_record ") {
                error {
                    check(group.id !in record.keys) { "ERROR: another record is ongoing in this group" }
                    val n = message.plainText().removePrefix("/start_record ")
                    check(n.isNotEmpty()) { "ERROR: empty record name" }
                    record[group.id] = Trace(n, sender.id, sender.nick, "")
                    quoteReply("DOMO, ${record[group.id]?.tellerName}=san. Motor·Rainbow, recorder circuit on, 実際安い.")
                }
                return@startsWith
            }
            case("/end_record") {
                record[group.id]?.let { t ->
                    if (t.tellerID == sender.id && t.tellerName == sender.nick) {
                        val file = t.recordString?.let { it1 -> writeTempFile(it1) }
                        file?.let { it1 -> upload(group, it1) }
                        file?.delete()
                        record.remove(group.id)
                        subject.sendMessage("Recorder circuit off. Uploading...")
                    }
                }

                return@case
            }
        }
    }
}