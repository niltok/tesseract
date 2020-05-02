package goldimax.tesseract

import com.jcabi.manifests.Manifests
import java.text.SimpleDateFormat
import java.util.*

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
    }
}
