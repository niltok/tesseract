package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

@ExperimentalStdlibApi
fun main() = runBlocking {
//    Log4j configure
    BasicConfigurator.configure()
    val bot = UniBot("conf.json", "env")

    bot.subscribeAll(
        listOf(
            Forward.invoke,
            { Picture(it) },
            ::qqOther,
            ::tgOther,
            { Counter(it) },
            { Markov(it) },
            { Reminder(it) }
        )
    ).start()
}

