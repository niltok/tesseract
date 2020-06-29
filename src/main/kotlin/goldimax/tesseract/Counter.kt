package goldimax.tesseract

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import net.mamoe.mirai.event.subscribeGroupMessages
import java.io.File

@ExperimentalStdlibApi
val counter = { uniBot: UniBot, confName: String ->
    File("$confName.json").run {
        if (!exists()) {
            createNewFile()
            // language: JSON
            writeText("{}")
        }
    }

    val json = getJson("$confName.json")

    val dic = json.map {
        it.key.toLong() to (it.value as JsonArray<*>).map {
            (it as JsonObject).string("regex")!!.toRegex() to it.int("count")!!
        }.toMap().toMutableMap()
    }.toMap().toMutableMap()

    fun save() {
        dic.forEach { (t, u) ->
            json[t.toString()] = JsonArray(u.map { (x, y) ->
                JsonObject(mapOf("regex" to x.pattern, "count" to y))
            })
        }
        putJson("$confName.json", json)
    }

    uniBot.qq.subscribeGroupMessages {
        startsWith("") {
            dic[source.group.id]?.run {
                forEach { (reg, v) -> if (reg matches it) {
                    set(reg, v + 1)
                    save()
                } }
            }
        }
    }

    uniBot.qq.subscribeGroupMessages {
        case("counter info") {
            error {
                val entry = dic[source.group.id]
                checkNotNull(entry) { "No counter info in this group." }

                reply("Counter Info\n" +
                        entry.map { (k, v) -> "$k : $v" }.joinToString("\n"))
            }
        }

        case("recount") {
            error {
                testSu(uniBot)

                val entry = dic[source.group.id]
                entry?.forEach { entry[it.key] = 0 }
                save()

                quoteReply("Done.")
            }
        }

        startsWith("add counter ", true) {
            error {
                testSu(uniBot)

                val entry = dic[source.group.id]
                if (entry == null) dic[source.group.id] = mutableMapOf(it.toRegex() to 0)
                else entry[it.toRegex()] = 0
                save()

                quoteReply("Done.")
            }
        }

        startsWith("remove counter ", true) {
            error {
                testSu(uniBot)

                dic[source.group.id]?.remove(it.toRegex())
                save()

                quoteReply("Done.")
            }
        }
    }

    Unit
}