package goldimax.tesseract

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.RemoteFile.Companion.uploadFile
import java.io.File
import kotlin.math.floor

data class Trace(
    var fileName: String? = null,
    var tellerID: Long = 0L,
    var tellerName: String? = null,
    var recordString: String? = null)

object Recorder {
    private var record : HashMap<Long, Trace> = HashMap<Long, Trace> ()
    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            always {
                if (record.keys.contains(group.id) && record[group.id]?.tellerID == sender.id && record[group.id]?.tellerName == sender.nick) {
                    val msg = message.content
                    if (msg != "/end_record") {
                        record[group.id]?.recordString += msg + "\n"
                        subject.sendMessage("√")
                    }
                }
                return@always
            }
            case("/start_record ") {
                error {
                    check(!record.keys.contains(group.id)) {"ERROR: another record is ongoing in this group"}
                    var n = message.plainText().removePrefix("/start_record ")
                    check(n.isNotEmpty()) { "ERROR: empty record name" }
                    record[group.id] = Trace("$n.txt", sender.id, sender.nick, "")
                    quoteReply("DOMO, ${record[group.id]?.tellerName}=san. Motor·Rainbow, recorder circuit on, 実際安い.")
                    }
                    return@case
                }
            case("/end_record") {
                if (sender.id ==  record[group.id]?.tellerID && sender.nick ==  record[group.id]?.tellerName) {
                    var file = File.createTempFile( record[group.id]?.fileName, null)
                    record[group.id]?.recordString?.let { it1 -> file.writeText(it1) }
                    group.filesRoot.resolve("/${record[group.id]?.fileName}").uploadAndSend(file)
                    var isDeleted = file.delete()
                    record.remove(group.id)
                    subject.sendMessage("Recorder circuit off. Uploading...")
                }
                return@case
            }
        }
    }
}