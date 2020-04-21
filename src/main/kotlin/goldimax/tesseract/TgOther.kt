package goldimax.tesseract

import com.jcabi.manifests.Manifests

fun tgOther(bot: UniBot) {
    bot.tg.onCommand("/rainbow") { msg, _ ->
        bot.tg.sendMessage(
            msg.chat.id,
            """
            Copy. I am online. 
            Here is ${msg.chat.id}.
            Build: ${Manifests.read("Version")}
            """.trimIndent()
        )
    }
}