package goldimax.tesseract

import com.alicloud.openservices.tablestore.model.*
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.PlainText
import java.lang.Math.random
import kotlin.math.*

@ExperimentalStdlibApi
class Markov(private val uniBot: UniBot) {
    private val rank = 3
    private val p: MutableMap<Long, Double> =
        uniBot.table.read("core", listOf("key" to "markov"))
        ?.get("p")?.asString()?.let { Klaxon().parse<Map<String, Double>>(it)
                ?.mapKeys { (k, _) -> k.toLong() } ?. toMutableMap() }
            ?: mutableMapOf()

    private fun train(text: String) {
        val rolls = (text zip text.drop(1)).map { (a, b) -> "$a$b" }
            .zip(text.drop(2).toList()).map { (a, b) -> "$a$b" }
            .zip(text.drop(3).map { "$it"})
        rolls.forEach {
            val m = getRoll(it.first) ?: mutableMapOf()
            val c = m[it.second] ?: 0L
            m[it.second] = c + 1
            putRoll(it.first, m)
        }
    }

    private fun putRoll(roll: String, m: MutableMap<String, Long>) {
        uniBot.table.write(
            "markov",
            listOf("prefix" to roll),
            listOf("main" to cVal(JsonObject(m).toJsonString())))
    }

    private fun gen(text: String): Pair<Boolean, String> {
        var ans = text
        var roll = text
        while (true) {
            val q = getRoll(roll) ?: return false to ans
            var n = ceil(random() * q.values.fold(0.toLong()) { acc, l -> acc + l }).toLong()
            var c = ""
            q.forEach { (t, u) ->
                n -= u
                if (n <= 0 && c == "") c = t
            }
            if (c == "。") return true to ans
            ans += c
            roll = (roll + c).drop(1)
            if (ans.length > 20) return false to ans
        }
    }

    private fun save() {
        uniBot.table.write("core", listOf("key" to "markov"),
            listOf("p" to cVal(JsonObject(p.mapKeys { (k, _) -> k.toString() }).toJsonString())))
    }

    init {
        uniBot.qq.subscribeGroupMessages {
            startsWith("PREFIX$", true) {
                error { reply("#" +
                        (getRoll(it.take(3))?.get(it.drop(3))?.toString() ?: "null")) }
            }
            startsWith("Mk$") {
                error {
                    val text = (message[PlainText]?.toString() ?: "").removePrefix("Mk$")
                    val g = gen(text.drop(floor(random() * (text.length - rank))
                         .toInt()).take(rank))
                    uniBot.qq.logger.debug("Markov" + g.second)
                    reply((if (g.first) "✓|" else "✗|") + g.second)
                }
            }
            startsWith("Mp$", true) {
                error {
                    testSu(uniBot)
                    p[source.group.id] = it.toDouble()
                    save()
                    reply("Done.")
                }
            }
            startsWith("") {
                val text = message[PlainText]?.toString() ?: ""
                train("$text。")
                if (text.length < 3 || random() < (1.0 - (p[source.group.id] ?: 0.0))) return@startsWith
                val g = gen(text.drop(floor(random() * (text.length - rank))
                    .toInt()).take(rank))
                if (g.first && g.second.length > rank) {
                    val qm = reply(g.second)
                    val tg = uniBot.connections.findTGByQQ(source.group.id)
                    if (tg != null)  uniBot.tg.sendMessage(tg, g.second).whenComplete { t, _ ->
                        uniBot.history.insert(qm.source, t.message_id)
                    }
                }
            }
        }

        uniBot.tgListener.add { s ->
            val text = s.text ?: ""
            train("$text。")
            if (text.length < 3 || random() < (1.0 - (p[uniBot.connections.findQQByTG(s.chat.id)] ?: 0.0))) return@add
            val g = gen(text.drop(floor(random() * (text.length - rank)).toInt()).take(rank))
            if (g.first && g.second.length > rank) {
                uniBot.tg.sendMessage(s.chat.id, g.second).whenComplete { t, _ ->
                    GlobalScope.launch {
                    val qq = uniBot.connections.findQQByTG(s.chat.id)
                    if (qq != null) uniBot.history.insert(
                        uniBot.qq.getGroup(qq).sendMessage(g.second).source, t.message_id)
                } }
            }
        }
    }

    private fun getRoll(roll: String): MutableMap<String, Long>? {
        return uniBot.table.read("markov", listOf("prefix" to roll))
            ?. get("main") ?. asString() ?. let { Klaxon().parse(it) }
    }
}