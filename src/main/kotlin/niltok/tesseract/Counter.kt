package niltok.tesseract

import net.mamoe.mirai.event.subscribeGroupMessages

object Counter {

    private val dic: MutableMap<Long, MutableMap<Regex, Long>> = mutableMapOf()

    private fun save() {
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