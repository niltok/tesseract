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
            { uniBot: UniBot -> picture(uniBot, "pic") },
            ::qqOther,
            ::tgOther,
            { counter(it, "count") },
            { Repeater.invoke(it, "repeater") },
            { Markov(it) },
            { Reminder.subscribe(it, "reminder") }
        )
    ).start()
}

