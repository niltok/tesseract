package goldimax.tesseract

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.RemoteFile.Companion.uploadFile
import java.io.File
import kotlin.math.floor

object Recorder {
    private var fileName = ""
    private var tellerID = 0L
    private var tellerName = ""
    private var recordingGroups : ArrayList<Long> = ArrayList()
    private var recordString : HashMap<Long, String> = HashMap<Long, String> ()
    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            always {
                if (recordingGroups.contains(group.id) && tellerID == sender.id && tellerName == sender.nick) {
                    val msg = message.content
                    if (msg != "/end_record") {
                        recordString.replace(group.id, recordString[group.id]+msg+"\n")
                        subject.sendMessage("√")
                    }
                }
                return@always
            }
            case("/start_record ") {
                error {
                    check(!recordingGroups.contains(group.id)) {"ERROR: another record is ongoing"}
                    fileName = message.plainText().removePrefix("/start_record ")
                    check(fileName.isNotEmpty()) { "ERROR: empty record name" }
                    fileName += ".txt"
                    tellerID = sender.id
                    tellerName = sender.nick
                    recordingGroups.add(group.id)
                    recordString[group.id] = ""
                    quoteReply("DOMO, ${tellerName}=san. Motor·Rainbow, recorder circuit on, 実際安い.")
                    }
                    return@case
                }
            case("/end_record") {
                if (sender.id == tellerID && sender.nick == tellerName) {
                    var file = File.createTempFile(fileName, null)
                    recordString[group.id]?.let { it1 -> file.writeText(it1) }
                    group.filesRoot.resolve("/$fileName").uploadAndSend(file)
                    var isDeleted = file.delete()
                    tellerID = 0L
                    tellerName = ""
                    recordingGroups.remove(group.id)
                    recordString.remove(group.id)
                    subject.sendMessage("Recorder circuit off. Uploading...")
                }
                return@case
            }
        }
    }
}