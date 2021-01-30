package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jcabi.manifests.Manifests
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

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

            onCommand("/hitokoto") { msg, _ ->
                val json = Parser.default().parse(
                    URL("https://v1.hitokoto.cn/")
                        .openStream()
                ) as JsonObject
                sendMessage(msg.chat.id, "「${
                    json.string("hitokoto")}」 —— ${
                    json.string("from")}")
            }

            onCommand("/disconnect") { msg, _ ->
                error(msg) {
                    testSu(msg)

                    Connections.connect.removeIf { it.tg == msg.chat.id }
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



            onCommand("/一言") { msg, cmd ->
                error (msg) {
                    val json = Parser.default().parse(
                        URL("https://v1.hitokoto.cn/")
                            .openStream()
                    ) as JsonObject
                    sendMessage(msg.chat.id, "「${
                        json.string("hitokoto")}」 —— ${
                        json.string("from")}")
                }
            }
        }
    }
}
