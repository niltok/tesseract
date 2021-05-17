package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.subscribeGroupMessages
import java.io.File

object Counter {
    private val json: Map<String, List<Map<String, String>>>? =
        getJson("core", "key", "counter", "json")

    private val dic = json?.map {
        it.key.toLong() to it.value.map {
            it["regex"]!!.toRegex() to it["counter"]!!.toLong()
        }.toMap().toMutableMap()
    }?.toMap()?.toMutableMap() ?: mutableMapOf()

    private fun save() {
        putJson("core", "key", "counter", "json",
            JsonObject(dic.mapKeys { it.key.toString() }.mapValues { JsonArray(it.value.map {
                    JsonObject(mapOf("regex" to it.key.pattern, "counter" to it.value.toString())) }) }))
    }

    init {
        UniBot.qq.eventChannel.subscribeGroupMessages {
            startsWith("") {
                dic[source.group.id]?.run {
                    forEach { (reg, v) ->
                        if (reg matches it) {
                            set(reg, v + 1)
                            save()
                        }
                    }
                }
            }
        }

        UniBot.qq.eventChannel.subscribeGroupMessages {
            case("counter info") {
                error {
                    val entry = dic[source.group.id]
                    checkNotNull(entry) { "No counter info in this group." }

                    reply("Counter Info\n" +
                            entry.map { (k, v) -> "$k : $v" }.joinToString("\n")
                    )
                }
            }

            case("recount") {
                error {
                    testSu()

                    val entry = dic[source.group.id]
                    entry?.forEach { entry[it.key] = 0 }
                    save()

                    quoteReply("Done.")
                }
            }

            startsWith("add counter ", true) {
                error {
                    testSu()

                    val entry = dic[source.group.id]
                    if (entry == null) dic[source.group.id] = mutableMapOf(it.toRegex() to 0L)
                    else entry[it.toRegex()] = 0
                    save()

                    quoteReply("Done.")
                }
            }

            startsWith("remove counter ", true) {
                error {
                    testSu()

                    dic[source.group.id]?.run {
                        filterKeys { reg -> reg.pattern == it }.forEach {
                            remove(it.key)
                        }
                    }
                    save()

                    quoteReply("Done.")
                }
            }
        }
    }
}