package niltok.tesseract

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import net.mamoe.mirai.event.subscribeGroupMessages
import java.lang.Math.random
import kotlin.math.*

object Markov {
    private const val rank = 3
    private var state = ""

    private fun getP(g: IMGroup) =
        db().hget("core:markov:p", SJson.encodeToString(g))?.toDoubleOrNull() ?: 0.0

    private fun setP(g: IMGroup, p: Double) {
        db().hset("core:markov:p", SJson.encodeToString(g), p.toString())
    }

    private fun train1(c: Char) {
        state += c
        if (state.length < rank + 1) return
        val m = getRoll(state.take(rank)) ?: mutableMapOf()
        val r = m[state.takeLast(1)] ?: 0L
        m[state.takeLast(1)] = r + 1L
        putRoll(state.take(rank), m)
        state = state.drop(1)
    }

    private fun train(text: String) {
        text.forEach { train1(it) }
    }

    private fun putRoll(roll: String, m: MutableMap<String, Long>) {
        db().hset("markov:data", roll, SJson.encodeToString(m))
    }
    private fun getRoll(roll: String): MutableMap<String, Long>? {
        return db().hget("markov:data", roll)?.let{
            SJson.decodeFromString(it)
        }
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
        UniBot.qq.eventChannel.subscribeGroupMessages {
            startsWith("PREFIX$", true) {
                error { reply("#" +
                        (getRoll(it.take(3))?.get(it.drop(3))?.toString() ?: "null")) }
            }
            startsWith("Mk$") {
                error {
                    val text = message.plainText().removePrefix("Mk$")
                    check(text.length >= 3) { "It's toooooo short." }
                    val g = gen(text.drop(floor(random() * (text.length - rank))
                         .toInt()).take(rank))
                    UniBot.qq.logger.debug("Markov" + g.second)
                    reply((if (g.first) "✓|" else "✗|") + g.second)
                }
            }
            startsWith("Mp$", true) {
                error {
                    testSu()
                    setP(group.toIMGroup(), it.toDouble())
                    quoteReply("Done.")
                }
            }
            always {
                val text = message.plainText().plus("。")
                train(text)
                if (text.length < 3 || random() < (1.0 - getP(group.toIMGroup()))) return@always
                val g = gen(text.drop(floor(random() * (text.length - rank))
                    .toInt()).take(rank))
                if (g.first && g.second.length > rank) {
                    val qm = reply(g.second)
                    val tg = Connections.findTGByQQ(source.group.id)
                    if (tg != null)  UniBot.tg.sendMessage(tg, g.second).whenComplete { t, _ ->
                        History.insert(qm.source, t)
                    }
                }
            }
        }

        UniBot.tgListener.add { s ->
            val text = s.text ?: ""
            train("$text。")
            if (text.length < 3 || random() < (1.0 - getP(IMGroup.TG(s.chat.id))))
                return@add
            val g = gen(text.drop(floor(random() * (text.length - rank)).toInt()).take(rank))
            if (g.first && g.second.length > rank) {
                UniBot.tg.sendMessage(s.chat.id, g.second).whenComplete { t, _ ->
                    runBlocking {
                        val qq = Connections.findQQByTG(s.chat.id) ?: return@runBlocking
                        val qGroup = UniBot.qq.getGroup(qq) ?: return@runBlocking
                        History.insert(qGroup.sendMessage(g.second).source, t)
                    }
                }
            }
        }

        UniBot.tg.run {
            onCommand("/mp") { msg, cmd ->
                error(msg) {
                    testSu(msg)
                    setP(IMGroup.TG(msg.chat.id), cmd!!.toDouble())
                    sendMessage(msg.chat.id, "Done.", replyTo = msg.message_id)
                }
            }

            onCommand("/mk") { msg, cmd ->
                error(msg) {
                    val text = (cmd ?: "").removePrefix("Mk$")
                    check(text.length >= 3) { "It's toooooo short." }
                    val g = gen(
                        text.drop(
                            floor(random() * (text.length - rank))
                                .toInt()
                        ).take(rank)
                    )
                    sendMessage(msg.chat.id, (if (g.first) "✓|" else "✗|") + g.second)
                }
            }
        }
    }
}