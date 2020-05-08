package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

@ExperimentalStdlibApi
fun main() = runBlocking<Unit> {
//    Log4j configure
    BasicConfigurator.configure()
    val bot = UniBot("conf.json")

    bot.subscribeAll(
        listOf(
            Forward.invoke,
            { uniBot: UniBot -> picture(uniBot, "pic") },
            ::qqOther,
            ::tgOther
        )
    ).start()
}

