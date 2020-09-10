package goldimax.tesseract

import com.jcabi.manifests.Manifests
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalStdlibApi
fun tgOther(bot: UniBot) {
    with(bot.tg) {
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

        onCommand("/connect") { msg, _ ->
            error {
                testSu(bot, msg)

                bot.connections.internal.value.add(Connection(msg.text!!.trim().toLong(), msg.chat.id))
                bot.save()

                sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
        }

        onCommand("/add_su") { msg, _ ->
            error {
                testSu(bot, msg)

                bot.suMgr.tgAdmin.add(msg.text!!.trim().toLong())
                bot.suMgr.save()

                sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
            }
        }
    }
}
