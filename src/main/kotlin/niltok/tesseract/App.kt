package niltok.tesseract

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {

    // Load Plugin
    UniBot
    QQOther
    TgOther
    Connections
    SUManager
    Forward
    Picture
    Reminder
    Markov
    WebPage
    Recorder
    FireWall

    // WebServer

    UniBot.start()
}

