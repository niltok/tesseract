package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jcabi.manifests.Manifests
import kotlinx.coroutines.coroutineScope
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.net.URL

object QQOther {
    init {
        val qq = UniBot.qq
        qq.eventChannel.subscribeMessages {
            case("rainbow") {
                val info = """
                Copy. I am online.${""/*Your ID is ${sender.id}.*/}
                You are${if (SUManager.isSuperuser(QQUser(sender.id))) "" else " not"} superuser.
                Build: ${Manifests.read("Version")}
            """.trimIndent()
                quoteReply(info)
            }
            startsWith("plz add su") {
                error {
                    testSu()

                    val id = message.plainText().removePrefix("plz add su").trim().let {
                        if (it.isBlank()) message.firstIsInstance<At>().target
                        else it.toLong()
                    }
                    SUManager.qqAdmin.add(id)
                    SUManager.save()
                    quoteReply("Done. $id has become superuser.")
                }
            }
            startsWith("is su") {
                error {
                    if (SUManager.isSuperuser(QQUser(message.firstIsInstance<At>().target))) quoteReply("Yes.")
                    else quoteReply("No.")
                }
            }
            case("一言") {
                error {
                    val json = Parser.default().parse(
                        URL("https://v1.hitokoto.cn/")
                            .openStream()
                    ) as JsonObject
                    reply("「${
                        json.string("hitokoto")}」 —— ${
                        json.string("from")}")
                }
            }
            case("kiss me") quoteReply (Face(Face.亲亲))
            case("mention all") reply (AtAll)
            case("reboot!!") {
                error {
                    testSu()
                    reply("Trying to reboot...")
                    Runtime.getRuntime().exec(
                        "systemctl restart rainbow.service")
                }
            }
            startsWith("") {
                if (message.plainText().trim() == "full content")
                    message[QuoteReply]?.source?.originalMessage?.let {
                        quoteReply(it.contentToString())
                    }
                message[RichMessage]?.content?.let {
                    val json = Parser.default().parse(it.byteInputStream()) as JsonObject
                    json.obj("meta") ?.values ?.map { it as JsonObject }
                        ?.joinToString("\n") { it.string("qqdocurl") ?: "" }
                        ?.let { quoteReply(it) }
                }
            }
        }
        qq.eventChannel.subscribeGroupMessages {
            startsWith("tex#", true) {
                error {
                    val img = WebPage.renderTex(it).inputStream().uploadAsImage(group)
                    reply(img)
                }
            }
            case("plz disconnect") {
                error {
                    testSu()

                    Connections.connect.removeIf { it.qq == source.group.id }
                    Connections.save()

                    reply("Done.")
                }
            }
            startsWith("") {
                try {
                    val url = URL(it)
                    if (url.host == "twitter.com")
                        reply(WebPage.renderTweet(it).inputStream().uploadAsImage(group))
                } catch (e : Exception) {}
            }
        }
    }
}