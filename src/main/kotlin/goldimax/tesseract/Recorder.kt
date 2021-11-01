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
    private var isRecording = false
    private var recordString = ""
    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            always {
                if (isRecording && tellerID == sender.id && tellerName == sender.nick) {
                    val msg = message.content
                    if (msg != "/end_record") {
                        recordString += msg + "\n"
                        subject.sendMessage("√")
                    }
                }
                return@always
            }
            case("/start_record ") {
                error {
                    check(!isRecording) {"ERROR: another record is ongoing"}
                    fileName = message.plainText().removePrefix("/start_record ")
                    check(fileName.isNotEmpty()) { "ERROR: empty record name" }
                    tellerID = sender.id
                    tellerName = sender.nick
                    isRecording = true
                    recordString = ""
                    subject.sendMessage("DOMO, ${tellerName}=san. Motor·Rainbow, recorder circuit on, 実際安い.")
                    }
                    return@case
                }
            case("/end_record") {
                if (sender.id == tellerID && sender.nick == tellerName) {
                    var file = File(fileName)
                    file.writeText(recordString)
                    group.uploadFile("/$fileName", file)
                    tellerID = 0L
                    tellerName = ""
                    isRecording = false
                    recordString = ""
                    subject.sendMessage("Recorder circuit off. Uploading...")
                }
                return@case
            }
        }
    }
}