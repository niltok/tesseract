package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

fun main() = runBlocking<Unit> {
//    Log4j configure
    BasicConfigurator.configure()
    val bot = UniBot("conf.json")

    val picture = Picture(bot, "pic")
    val forward = Forward(bot)
    qqOther(bot)
    tgOther(bot)

    bot.join()
}

