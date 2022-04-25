package niltok.tesseract

import com.jcabi.manifests.Manifests
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.net.URL

object QQOther {
    init {
        val qq = UniBot.qq
        qq.eventChannel.subscribe<BotInvitedJoinGroupRequestEvent> {
            accept()
            ListeningStatus.LISTENING
        }
        qq.eventChannel.subscribeMessages {
            case("rainbow") {
                val info = """
                Copy. I am online.${""/*Your ID is ${sender.id}.*/}
                You are${if (SUManager.isSuperuser(IMUser.QQ(sender.id))) "" else " not"} superuser.
                Build: ${Manifests.read("Version")}
            """.trimIndent()
                quoteReply(info)
            }
            case("plz list joined groups") {
                error {
                    testSu()
                    reply(bot.groups.joinToString("\n") { "${it.name}(${it.id})" })
                }
            }
            startsWith("plz add su") {
                error {
                    testGSu()

                    val id = message.plainText().removePrefix("plz add su").trim().let {
                        if (it.isBlank()) message.firstIsInstance<At>().target
                        else it.toLong()
                    }
                    SUManager.addSuperuser(IMUser.QQ(id))
                    quoteReply("Done. $id has become superuser.")
                }
            }
            startsWith("is su") {
                error {
                    if (SUManager.isSuperuser(IMUser.QQ(message.firstIsInstance<At>().target))) quoteReply("Yes.")
                    else quoteReply("No.")
                }
            }
            case("一言") {
                error {
                    val json = SJson.parseToJsonElement(
                        URL("https://v1.hitokoto.cn/")
                        .openStream().readBytes().decodeToString()).jsonObject
                    reply("「${
                        json["hitokoto"]?.jsonPrimitive?.contentOrNull
                    }」 —— ${
                        json["from"]?.jsonPrimitive?.contentOrNull
                    }")
                }
            }
            case("kiss me") quoteReply (Face(Face.亲亲))
            case("mention all") reply (AtAll)
            startsWith("tex#", true) {
                error {
                    subject.sendMessage(WebPage.renderTex(it).inputStream().uploadAsImage(subject))
                }
            }
            always {
                if (message.plainText().trim() == "full content")
                    message[QuoteReply]?.source?.originalMessage?.let {
                        quoteReply(it.contentToString())
                    }
                message[RichMessage]?.content?.let {
                    val json = SJson.parseToJsonElement(it).jsonObject
                    json["meta"]?.jsonObject?.values
                        ?.joinToString("\n") {
                            it.jsonObject["qqdocurl"]?.jsonPrimitive?.contentOrNull ?: ""
                        }
                        ?.let { quoteReply(it) }
                }
                try {
                    val url = URL(it)
                    if (url.host == "twitter.com")
                        subject.sendMessage(WebPage.renderTweet(it).inputStream().uploadAsImage(subject))
                } catch (e : Exception) {}
            }
        }
    }
}