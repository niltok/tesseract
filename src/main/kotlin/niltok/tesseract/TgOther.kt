package niltok.tesseract

import com.jcabi.manifests.Manifests
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
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
            You are ${msg.from!!.id}.
            You are${if (SUManager.isSuperuser(IMUser.TG(msg.from!!.id.toLong()))) "" else " not"} superuser.
            Here is ${msg.chat.id}.
            Build: $version
            """.trimIndent()
                )
            }

            onCommand("/hitokoto") { msg, _ ->
                val json = SJson.parseToJsonElement(
                    URL("https://v1.hitokoto.cn/")
                        .openStream().readBytes().decodeToString()).jsonObject
                sendMessage(msg.chat.id, "「${
                    json["hitokoto"]?.jsonPrimitive?.contentOrNull
                }」 —— ${
                    json["from"]?.jsonPrimitive?.contentOrNull
                }")
            }

            onCommand("/add_su") { msg, cmd ->
                error(msg) {
                    testSu(msg)

                    SUManager.addSuperuser(IMUser.TG(cmd!!.trim().toLong()))

                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }
        }
        UniBot.tgListener.add {
            try {
                val qq = UniBot.qq.getGroup(Connections.findQQByTG(it.chat.id)!!)!!
                val url = it.entities!!.first { URL(it.url).host == "twitter.com" }
                qq.sendImage(WebPage.renderTweet(url.toString()).inputStream())
            } catch (e : Exception) {}
        }
    }
}
