package goldimax.tesseract

import kotlinx.coroutines.*

fun main() = runBlocking<Unit> {
    val bot = UniBot("conf.json")

    val picture = Picture(bot, "pic")
    val forward = Forward(bot)
    qqOther(bot)
    tgOther(bot)

    bot.join()
}

