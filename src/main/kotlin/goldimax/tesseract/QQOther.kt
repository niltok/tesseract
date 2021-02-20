package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jcabi.manifests.Manifests
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
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
                        if (it.isEmpty()) message.firstIsInstance<At>().target
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
                        "killall java; nohup java -jar rainbow.jar &")
                }
            }
        }
        qq.eventChannel.subscribeGroupMessages {
            case("plz disconnect") {
                error {
                    testSu()

                    Connections.connect.removeIf { it.qq == source.group.id }
                    Connections.save()

                    reply("Done.")
                }
            }
        }
    }
}