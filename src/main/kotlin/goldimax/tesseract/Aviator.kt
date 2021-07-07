package goldimax.tesseract

import com.beust.klaxon.JsonObject
import com.googlecode.aviator.AviatorEvaluator
import com.googlecode.aviator.Feature
import com.googlecode.aviator.Options
import com.googlecode.aviator.runtime.JavaMethodReflectionFunctionMissing
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.QuoteReply
import net.mamoe.mirai.message.data.firstIsInstanceOrNull
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.*

object Aviator {
    object ScriptMgr {
        operator fun get(s: UUID) =
            UniBot.table.read("aviator", listOf("uuid" to s.toString()))?.get("code")?.asString()
        operator fun set(s: UUID, c: String) =
            UniBot.table.write("aviator", listOf("uuid" to s.toString()), listOf("code" to cVal(c)))
        fun remove(s: UUID) =
            UniBot.table.remove("aviator", listOf("uuid" to s.toString()))
        fun new(c: String) = UUID.randomUUID()!!.also { set(it, c) }
    }

    val scripts =
        (getJson("core", "key", "aviator", "json")
                as Map<String, List<Map<String, String>>>?)?.let {
            it.mapKeys { it.key.toLong() } .mapValues { it.value.map {
                it["name"]!! to UUID.fromString(it["uuid"])!! }.toMap().toMutableMap()
            } .toMutableMap()} ?: mutableMapOf()

    fun save() {
        putJson("core", "key", "aviator", "json",
            JsonObject(scripts.mapKeys { it.key.toString() } .mapValues { it.value.map {
                mapOf("name" to it.key, "uuid" to it.value) } })
        )
    }

    val avs = AviatorEvaluator.newInstance()
    val loopLimit = 1000
    val allowedClass = listOf(
        java.lang.String::class.java,
        Math::class.java,
        java.util.Objects::class.java,
        java.util.Collection::class.java,
        java.util.List::class.java,
        java.util.Set::class.java,
        java.util.Map::class.java,
        java.util.Iterator::class.java,
        BitSet::class.java,
        Base64::class.java,
        Date::class.java,
        Calendar::class.java,
        Instant::class.java,
        Random::class.java,
        UUID::class.java,
        Formatter::class.java,
        Scanner::class.java,
        InputStream::class.java,
        OutputStream::class.java
    )
    suspend fun MessageEvent.avsExec(s: String) =
        quoteReply(avs.execute(s)?.toString() ?: "null")

    init {
        avs.disableFeature(Feature.Module)
        avs.setOption(Options.MAX_LOOP_COUNT, loopLimit)
        avs.setOption(Options.ALLOWED_CLASS_SET, allowedClass.toHashSet())
        avs.functionMissing = JavaMethodReflectionFunctionMissing.getInstance()
        UniBot.qq.eventChannel.subscribeMessages {
            "avs status" reply """
                禁用语法特性: Module
                循环次数限制: $loopLimit
                Class 白名单: 
                  ${allowedClass.joinToString("\n                " + "  ") { it.name }}
            """.trimIndent()
            "avs help" reply """
                只能运行自己保存的代码，但可以查看其他人保存的代码
                语言帮助文档: https://www.yuque.com/boyan-avfmj/aviatorscript
                
                avs help
                avs help help
                avs status
                avs#<code>
                {reply} avs run (<code>)
                avs run [<name>]
                {reply} avs save <name>
                avs list ({mention})
                avs delete <name>
                avs show ({mention}) [<name>]
            """.trimIndent()
            "avs help help" reply """
                {reply} 要求 消息回复
                {mention} 要求 @一位成员
                <name> <code> 表示 参数
                    其中 <name> 作为参数时其中不能包含 ':', ' ', '#'
                [<name>] 表示 参数组
                ({mention}) 表示可选内容
                // command 表示 在做了.jpg
            """.trimIndent()
            startsWith("avs#") {
                error { avsExec(it) }
            }
            startsWith("avs run") {
                error {
                    avsExec(message[QuoteReply]?.let { msg ->
                        it + msg.source.originalMessage.plainText()
                    } ?: it.split(" ").map { it.trim() }.joinToString("\n") {
                        ScriptMgr[scripts[sender.id]?.get(it) ?: error("Cannot find $it")]!!
                    })
                }
            }
            startsWith("avs save") {
                error {
                    scripts[sender.id] = scripts[sender.id] ?: mutableMapOf()
                    scripts[sender.id]!![it] =  ScriptMgr.new(
                        message[QuoteReply]?.source?.originalMessage?.plainText() ?: error("Code plz"))
                    save()
                    quoteReply("Done.")
                }
            }
            startsWith("avs list") {
                error {
                    scripts[message.firstIsInstanceOrNull<At>()?.target ?: sender.id]?.keys?.let {
                        quoteReply(it.joinToString("\n"))
                    } ?: quoteReply("Empty.")
                }
            }
            startsWith("avs delete") {
                error {
                    ScriptMgr.remove(scripts[sender.id]?.get(it) ?: error("No such a script"))
                    scripts[sender.id]?.remove(it)
                    save()
                    quoteReply("Done.")
                }
            }
            startsWith("avs show") {
                error {
                    quoteReply(message.plainText().removePrefix("avs show").split(" ")
                        .map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n") {
                            ScriptMgr[scripts[message.firstIsInstanceOrNull<At>()?.target ?: sender.id]
                                ?.get(it) ?: error("Cannot find $it")]!!
                        })
                }
            }
        }
    }
}