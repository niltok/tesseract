package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

fun main() = runBlocking<Unit> {
//    Log4j configure
    BasicConfigurator.configure()
    val bot = UniBot("conf.json")

    Picture(bot, "pic")
    qqOther(bot)
    tgOther(bot)

    bot.subscribes.addAll(
        listOf(
            forward
        )
    )

    bot.start()
}

