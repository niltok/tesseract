package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

@ExperimentalStdlibApi
fun main() = runBlocking {
    // Log4j configure
    BasicConfigurator.configure()

    // Load Plugin
    UniBot
    TransactionManager
    QQOther
    TgOther
    Forward
    Picture
    Counter
    Reminder
    Alarm
    Markov

    UniBot.start()
}

