package goldimax.tesseract

import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator

fun main() = runBlocking<Unit> {
//    System.setProperty("java.net.useSystemProxies", "true");
//    System.setProperty("socksProxyHost", "192.168.1.6");
//    System.setProperty("socksProxyPort", "7891");
//    Log4j configure
    BasicConfigurator.configure()
    val bot = UniBot("conf.json")

    val picture = Picture(bot, "pic")
    val forward = Forward(bot)
    qqOther(bot)
    tgOther(bot)

    bot.join()
}

