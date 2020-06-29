package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jcabi.manifests.Manifests
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.*
import java.net.URL

@ExperimentalStdlibApi
fun qqOther(bot: UniBot) {
    val qq = bot.qq
    qq.subscribeMessages {
        case("rainbow") {
            val info = """
                Copy. I am online.${""/*Your ID is ${sender.id}.*/}
                You are${if (bot.suMgr.isSuperuser(QQUser(sender.id))) "" else " not"} superuser.
                Build: ${Manifests.read("Version")}
            """.trimIndent()
            quoteReply(info)
        }
        startsWith("plz add su") {
            error {
                testSu(bot)

                val id = message[PlainText].toString().removePrefix("plz add su").trim().let {
                    if (it.isEmpty()) message[At]!!.target
                    else it.toLong()
                }
                bot.suMgr.qqAdmin.add(id)
                bot.suMgr.save()
                quoteReply("Done. $id has become superuser.")
            }
        }
        startsWith("is su") {
            error {
                if (bot.suMgr.isSuperuser(QQUser(message[At]!!.target))) quoteReply("Yes.")
                else quoteReply("No.")
            }
        }
        case("一言") {
            error {
                val json = Parser.default().parse(
                    URL("https://v1.hitokoto.cn/")
                        .openStream()
                ) as JsonObject
                reply("「${json.string("hitokoto")}」 —— ${json.string("from")}")
            }
        }
        case("kiss me") quoteReply (Face(Face.qinqin))
    }
}