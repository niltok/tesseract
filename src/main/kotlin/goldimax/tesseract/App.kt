package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

fun main() = runBlocking {
    // Log4j configure
    BasicConfigurator.configure()

    // Load Plugin
    UniBot
    QQOther
    TgOther
    Forward
    Picture
    Counter
    Reminder
    Alarm
    Markov

    WebServer

    UniBot.start()
}

