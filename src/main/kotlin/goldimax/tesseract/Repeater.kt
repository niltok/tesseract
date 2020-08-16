package goldimax.tesseract

import com.beust.klaxon.*
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.PlainText
import java.io.File
import java.time.Duration
import java.util.*

const val learningRate = 0.1
const val limit = 0.0

private interface Layer {
    fun dump(): JsonObject
    fun run(input: List<Double>): List<Double>
    fun train(delta: List<Double>, a: List<Double>): List<Double>
}

private interface Creator {
    fun undump(json: JsonObject): Layer
    val default: () -> Layer
}

private typealias Network = List<Layer>

private fun Network.run(input: List<Double>) =
    fold(input) { acc, x -> x.run(acc) }

@ExperimentalStdlibApi
private fun Network.train(input: List<Double>, output: List<Double>) {
    val process = scan(input) { acc, x -> x.run(acc) }
    (this zip process).foldRight(
        (output zip process.last()).map { (y, a) -> y - a }
    ) { (x, a), acc -> x.train(acc, a) }
}

private fun Network.dump() =
    JsonArray(map { it.dump() })

private class Blank: Layer {
    override fun dump() = JsonObject()
    override fun run(input: List<Double>) = input
    override fun train(delta: List<Double>, a: List<Double>) = delta
}

private class BlankCreator: Creator {
    override fun undump(json: JsonObject) = Blank()
    override val default = { Blank() }
}

private class ReLU: Layer {
    private fun relu (x: Double): Double = if (x > 0.0) x   else 0.0
    private fun relu_(x: Double): Double = if (x > 0.0) 1.0 else 0.0

    override fun dump() = JsonObject()
    override fun run(input: List<Double>) = input.map { relu(it) }
    override fun train(delta: List<Double>, a: List<Double>) =
        (delta zip a).map { (x, y) -> x * relu_(y) }
}

private class ReLUCreator: Creator {
    override fun undump(json: JsonObject) = ReLU()
    override val default = { ReLU() }
}

private class Matrix(private var matrix: List<List<Double>>): Layer {
    override fun dump() = JsonObject(mapOf("matrix" to JsonArray(matrix.map { JsonArray(it) })))
    override fun run(input: List<Double>) = matrix
        .map { (it zip input) .map { (x, y) -> x * y } .reduce { x, y -> x + y } }
    override fun train(delta: List<Double>, a: List<Double>): List<Double> {
        val ans = (matrix zip delta)
            .map { (list, v) -> list.map { it * v } }
            .reduce { acc, v -> (acc zip v).map { (x, y) -> x + y } }
        matrix = (matrix zip a)
            .map { (list, v) -> list.map { it - learningRate * v } }
        return ans
    }
}

private class MatrixCreator(override val default: () -> Matrix): Creator {
    override fun undump(json: JsonObject) =
        Matrix(json.array<JsonArray<Double>>("matrix")!!.map { it.toList() })
}

private class Addition(private var a: List<Double>): Layer {
    override fun dump() = JsonObject(mapOf("a" to JsonArray(a)))
    override fun run(input: List<Double>) = (a zip input).map { (u, v) -> u + v }
    override fun train(delta: List<Double>, a: List<Double>): List<Double> {
        this.a = (this.a zip delta).map { (u, v) -> u - learningRate * v }
        return delta
    }
}

private class AdditionCreator(override val default: () -> Addition): Creator {
    override fun undump(json: JsonObject) =
        Addition(json.array<Double>("a")!!.toList())
}

private class NetworkCreator(val Creators: List<Creator>) {
    fun create(json: JsonArray<JsonObject>) = (json.toList() zip Creators)
        .map { (j, c) -> c.undump(j) }
    fun default() = Creators.map { it.default() }
}

@ExperimentalStdlibApi
object Repeater {
    private var network: Network = emptyList()
    private var creator = NetworkCreator(listOf(
        MatrixCreator { Matrix((0 until 30).map { (0 until 100).map { 0.0 } }) },
        AdditionCreator { Addition((0 until 100).map { 0.0 }) },
        ReLUCreator(),
        MatrixCreator { Matrix((0 until 100).map { (0 until 100).map { 0.0 } }) },
        AdditionCreator { Addition((0 until 100).map { 0.0 }) },
        ReLUCreator(),
        MatrixCreator { Matrix((0 until 100).map { (0 until 100).map { 0.0 } }) },
        AdditionCreator { Addition((0 until 100).map { 0.0 }) },
        ReLUCreator(),
        MatrixCreator { Matrix((0 until 100).map { (0 until 2).map { 0.0 } }) },
        ReLUCreator(),
        BlankCreator()
    ))
    private val history = mutableListOf<Pair<String, Date>>()
    private val counter = mutableMapOf<String, Int>()
    private var repeat = false
    private fun mkList(s: String): List<Double> {
        val list = s.map { it.toDouble() * 0.01 } .toMutableList()
        while (list.size < 20) list.add(0.0)
        return list.toList()
    }
    val invoke = { uniBot: UniBot, conf: String ->
        if (!File("$conf.json").exists()) {
            network = creator.default()
            File("$conf.json").run {
                createNewFile()
                writeText(JsonObject(mapOf("base" to network.dump()))
                    .toJsonString(true))
            }
        } else {
            network = creator.create(getJson("$conf.json").array("base")!!)
        }
        uniBot.qq.subscribeMessages {
            case("turn on repeater") { repeat = true; reply("Done.") }
            case("turn off repeater") { repeat = false; reply("Done.") }
            startsWith("") {
                val text = message[PlainText]!!.contentToString()
                if (text.length > 30) return@startsWith
                history.add(text to Date())
                counter[text] = (counter[text] ?: 0) + 1

                while (history.isNotEmpty() &&
                    history.first().second.toInstant() + Duration.ofMinutes(20)
                    < Date().toInstant()) {

                    val s = history.first().first
                    network.train(mkList(history.first().first),
                        listOf(0.0, counter[s]!!.toDouble() - 1.0))
                    putJson("$conf.json", JsonObject(mapOf("base" to network.dump())))
                    counter[s] = counter[s]!!.minus(1)
                    if (counter[s] == 0) counter.remove(s)
                    history.removeFirst()
                }

                val result = network.run(mkList(text))
                if (result[1] - result[0] > limit) reply(text)
            }
        }
        Unit
    }
}