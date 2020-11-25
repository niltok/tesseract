package goldimax.tesseract

import com.jcabi.manifests.Manifests
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalStdlibApi
object TgOther {
    init {
        with(UniBot.tg) {
            onCommand("/rainbow") { msg, _ ->
                val version = try {
                    Manifests.read("Version")
                } catch (e: IllegalArgumentException) {
                    SimpleDateFormat("yyyy/M/dd HH:mm:ss").format(Date())
                }

                sendMessage(
                    msg.chat.id,
                    """
            Copy. I am online.
            You are ${msg.from?.id}.
            Here is ${msg.chat.id}.
            Build: $version
            """.trimIndent()
                )
            }

            onCommand("/connect") { msg, cmd ->
                error(msg) {
                    testSu(msg)

                    Connections.connect.add(Connection(cmd!!.trim().toLong(), msg.chat.id))
                    Connections.save()

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }

            onCommand("/add_su") { msg, cmd ->
                error(msg) {
                    testSu(msg)

                    SUManager.tgAdmin.add(cmd!!.trim().toLong())
                    SUManager.save()

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }
    }
}
