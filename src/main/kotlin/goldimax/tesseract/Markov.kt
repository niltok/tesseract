package goldimax.tesseract

import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.PlainText
import java.io.File
import java.lang.Math.random
import kotlin.math.*

@ExperimentalStdlibApi
object Markov {
    val rank = 3
    var data = mutableMapOf<String, MutableMap<String, Long>>()

    fun train(text: String) {}

    fun gen(text: String): Pair<Boolean, String> {
        var ans = text
        var roll = text
        while (true) {
            val q = data[roll] ?: return false to ans
            var n = ceil(random() * q["tt"]!!).toLong()
            var c = ""
            q.forEach { (t, u) ->
                if (t != "tt") n -= u
                if (n <= 0 && c == "") c = t
            }
            if (c == "。") return true to ans
            ans += c
            roll = (roll + c).drop(1)
            if (ans.length > 20) return false to ans
        }
    }

    val subscribe = { uniBot: UniBot, confName: String ->
        File("$confName.json").run {
            if (!exists()) {
                createNewFile()
                writeText("{}")
            }
        }
        data = getJson("$confName.json")
            .map { (k, v) ->
            k to (v as JsonObject).map { (k, v) -> k to (v as Long) }.toMap().toMutableMap()
        }.toMap().toMutableMap()

        uniBot.qq.subscribeMessages {
            startsWith("PREFIX$", true) {
                error { reply("#" +
                        (data[it.take(3)]?.get(it.drop(3))?.toString() ?: "null")) }
            }
            startsWith("Mk$") {
                error {
                    val text = message[PlainText].toString().removePrefix("Mk$")
                    val g = gen(text.take(rank))
                    uniBot.qq.logger.debug("Markov" + g.second)
                    reply((if (g.first) "✓|" else "✗|") + g.second)
                }
            }
            startsWith("") {
                val text = message[PlainText].toString()
                train("$text。")
                val g = gen(text.drop(floor(random() * (text.length - rank)).toInt()).take(rank))
                if (g.first && g.second.length > 3) reply(g.second)
            }
        }

        Unit
    }
}