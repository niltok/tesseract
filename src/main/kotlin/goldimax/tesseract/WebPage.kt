package goldimax.tesseract

import com.ruiyun.jvppeteer.core.Puppeteer
import com.ruiyun.jvppeteer.core.browser.BrowserFetcher
import com.ruiyun.jvppeteer.core.page.JSHandle
import com.ruiyun.jvppeteer.options.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import org.apache.log4j.BasicConfigurator
import java.io.File
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis

fun <T> printTime(f : () -> T) : T {
    var res : T
    println(measureTimeMillis { res = f() })
    return res
}

object WebPage {
    val browser = run {
        BrowserFetcher.downloadIfNotExist(null)
        Puppeteer.launch(LaunchOptionsBuilder().withArgs(listOf("--no-sandbox")).withHeadless(true).build())
    }

    val screenshotOptions = run {
        val option = ScreenshotOptions()
        option.type = "jpeg"
        option.quality = 95
        option
    }

    val latexPage = run {
        val page = browser.newPage()!!
        val version = "0.13.13"
        //language=HTML
        page.setContent("""<html>
            <head>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_AMS-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Caligraphic-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Caligraphic-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Fraktur-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Fraktur-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Main-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Main-BoldItalic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Main-Italic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Main-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Math-BoldItalic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Math-Italic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_SansSerif-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_SansSerif-Italic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_SansSerif-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Script-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Size1-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Size2-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Size3-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Size4-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/fonts/KaTeX_Typewriter-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <script src="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/katex.min.js"></script>
            <link href="https://cdn.bootcdn.net/ajax/libs/KaTeX/$version/katex.min.css" rel="stylesheet">
            <script>
                let loaded = false
                document.onload = e => { loaded = true }
            </script>
            <style>
                .box {
                    padding: 1em; 
                    display: inline-block;
                }
            </style>
            </head>
            <body></body>
        </html>""")
        page.setViewport(
            Viewport(5000, 5000, 3,
                false, false, false)
        )
        page
    }
    val latexMutex = Mutex()

    suspend fun renderTex(tex : String) : ByteArray {
        latexPage.waitForFunction("""
            typeof(loaded) != 'undefined' && loaded
        """)
        return latexMutex.withLock {
            val id = UUID.randomUUID().toString()
            latexPage.evaluate("""
                (function () {
                    let e = document.createElement('div')
                    e.id = "box$id"
                    e.className = "box"
                    e.innerHTML = katex.renderToString(String.raw`$tex`, {throwOnError: false})
                    document.body.append(e)
                })()
            """)
            latexPage.waitForSelector("#box$id > .katex")
            val katex = latexPage.waitForSelector("#box$id")
            val image = Base64.getDecoder().decode(katex.screenshot(screenshotOptions))!!
            latexPage.evaluate("""
                document.querySelector("#box$id").remove()
            """)
            image
        }
    }

    fun renderTweet(url : String) : ByteArray {
        val page = browser.newPage()
        page.setViewport(
            Viewport(1080, 5000, 1.5,
            false, false, false)
        )
        page.goTo(url)
        page.waitForSelector("article")
        val tweet = page.waitForFunction("""
            (() => 
                Array.from(document.querySelectorAll("article")).find(e => 
                    typeof(e.attributes.tabindex) == 'undefined')
            )
        """)?.asElement() ?: throw Throwable("not tweet")
        val image = Base64.getDecoder().decode(tweet.screenshot(screenshotOptions))
        page.close()
        return image
    }
}

suspend fun main() {
    BasicConfigurator.configure()
    File("tex.png").writeBytes(WebPage.renderTex("""
        \LaTeX\texttt{LaTeX}\textit{LaTeX}\textrm{LaTeX}
    """))
    File("tweet.png").writeBytes(WebPage.renderTweet("""
        https://twitter.com/NiltokotliN/status/1364286677305368576
    """.trimIndent()))
    println("Done.")
}