package goldimax.tesseract

import com.alicloud.openservices.tablestore.model.*
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.PlainText
import java.lang.Math.random
import kotlin.math.*

@ExperimentalStdlibApi
class Markov(private val uniBot: UniBot) {
    private val rank = 3

    private fun train(text: String) {
        val rolls = (text zip text.drop(1)).map { (a, b) -> "$a$b" }
            .zip(text.drop(2).toList()).map { (a, b) -> "$a$b" }
            .zip(text.drop(3).map { "$it"})
        rolls.forEach {
            val m = getRoll(it.first) ?: mutableMapOf()
            val c = m[it.second] ?: 0
            m[it.second] = c + 1
            putRoll(it.first, m)
        }
    }

    private fun putRoll(roll: String, m: MutableMap<String, Long>) {
        println("putRoll $" + roll + ": " + JsonObject(m).toJsonString())
        uniBot.table.putRow(PutRowRequest(RowPutChange("markov",
            PrimaryKey(listOf(PrimaryKeyColumn("prefix",
                PrimaryKeyValue.fromString(roll)))))
            .addColumn("main", ColumnValue.fromString(JsonObject(m).toJsonString()))))
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

    init {
        uniBot.qq.subscribeMessages {
            startsWith("PREFIX$", true) {
                error { reply("#" +
                        (getRoll(it.take(3))?.get(it.drop(3))?.toString() ?: "null")) }
            }
            startsWith("Mk$") {
                error {
                    val text = (message[PlainText]?.toString() ?: "").removePrefix("Mk$")
                    val g = gen(text.take(rank))
                    uniBot.qq.logger.debug("Markov" + g.second)
                    reply((if (g.first) "✓|" else "✗|") + g.second)
                }
            }
            startsWith("") {
                if (random() > 0.25) return@startsWith
                val text = message[PlainText]?.toString() ?: ""
                train("$text。")
                val g = gen(text.drop(floor(random() * (text.length - rank - 1)).toInt()).take(rank))
                if (g.first && g.second.length > 3) reply(g.second)
            }
        }

        Unit
    }

    private fun getRoll(roll: String): MutableMap<String, Long>? {
        val client = uniBot.table
        val query = SingleRowQueryCriteria("markov",
            PrimaryKey(listOf(PrimaryKeyColumn("prefix", PrimaryKeyValue.fromString(roll)))))
        query.maxVersions = 1
        return client.getRow(GetRowRequest(query)).row
            ?. getColumn("main") ?. firstOrNull() ?. value ?. asString()
            ?. let { Klaxon().parse(it) }
    }
}