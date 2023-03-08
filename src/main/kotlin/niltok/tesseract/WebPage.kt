package niltok.tesseract

import com.ruiyun.jvppeteer.core.Puppeteer
import com.ruiyun.jvppeteer.options.LaunchOptionsBuilder
import com.ruiyun.jvppeteer.options.ScreenshotOptions
import com.ruiyun.jvppeteer.options.Viewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.*
import java.util.stream.Collectors
import kotlin.system.measureTimeMillis

inline fun <T> printTime(prefix: String, f : () -> T) : T {
    var res : T
    println("[$prefix Time]: ${ measureTimeMillis { res = f() } }ms")
    return res
}

var headless = true

object WebPage {
    val browser = run {
        Puppeteer.launch(LaunchOptionsBuilder()
            .withExecutablePath("/usr/bin/chromium")
            .withArgs(listOf("--no-sandbox"))
            .withHeadless(headless)
            .build())
    }

    val jpgShot = run {
        val option = ScreenshotOptions()
        option.type = "jpeg"
        option.quality = 95
        option
    }

    val latexMutex = Mutex()
    val latexPage = runBlocking {
        val page = browser.newPage()!!
        val version = "0.15.2"
        latexMutex.lock()
        page.onLoad {
            runBlocking { latexMutex.unlock() }
        }
        //language=HTML
        page.setContent("""<html>
            <head>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_AMS-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Caligraphic-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Caligraphic-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Fraktur-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Fraktur-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Main-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Main-BoldItalic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Main-Italic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Main-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Math-BoldItalic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Math-Italic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_SansSerif-Bold.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_SansSerif-Italic.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_SansSerif-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Script-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Size1-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Size2-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Size3-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Size4-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <link rel="preload" href="https://cdn.jsdelivr.net/npm/katex@$version/dist/fonts/KaTeX_Typewriter-Regular.woff2" 
                as="font" type="font/woff2" crossorigin>
            <script async src="https://cdn.jsdelivr.net/npm/katex@$version/dist/katex.min.js"></script>
            <link href="https://cdn.jsdelivr.net/npm/katex@$version/dist/katex.min.css" rel="stylesheet">
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

    suspend fun renderTex(tex : String) : ByteArray = latexMutex.withLock {
        println("Render Tex $tex")
        val id = UUID.randomUUID().toString()
        latexPage.evaluate("""
            (function () {
                let e = document.createElement('div')
                e.id = "box$id"
                e.className = "box"
                e.innerHTML = katex.renderToString(`${
                    tex.codePoints().boxed().map { String.format("\\u%04x", it) }.collect(Collectors.joining())
                }`, {displayMode: true})
                document.body.append(e)
            })()
        """)
        println("Tex added to DOM")
        doIO { latexPage.waitForSelector("#box$id > .katex-display") }
        val katex = doIO { latexPage.waitForSelector("#box$id") }
        println("Screenshotting Tex")
        val image = Base64.getDecoder().decode(doIO { katex.screenshot(jpgShot) })!!
        latexPage.evaluate("""
            document.querySelector("#box$id").remove()
        """)
        println("Tex Rendered")
        image
    }

    private const val gfps = 30
    val lottieMutex = Mutex()
    val lottiePage = runBlocking {
        val page = browser.newPage()!!
        lottieMutex.lock()
        page.onLoad { lottieMutex.unlock() }
        //language=HTML
        page.setContent(
            """
            <html>
            <head>
            <script src="https://cdn.jsdelivr.net/npm/lottie-web@5.8.1/build/player/lottie.min.js"></script>
            <script async src="https://cdn.jsdelivr.net/npm/gif.js@0.2.0/dist/gif.js"></script>
            </head>
            <body>
            <script>
            let gif_worker = null;
            function renderGif(gif) {
                return new Promise((res, rej) => {
                    gif.on("finished", blob => {
                        let reader = new FileReader()
                        reader.onload = e => {
                            res(e.target.result)
                        }
                        reader.onerror = ev => rej(ev)
                        reader.readAsDataURL(blob)
                    })
                    gif.render()
                })
            }
            function wait(ms) {
                return new Promise((res, rej) => {
                    let r = setTimeout(res, ms)
                    if (r === 0) rej(r)
                })
            }
            async function genGif(script, fps, scale) {
                if (!scale) scale = 1
                let root = document.createElement('div')
                document.body.append(root)
                root.style.height = script['h'] * scale
                root.style.width = script['w'] * scale
                root.style.position = 'absolute'
                let animation = lottie.loadAnimation({
                    container: root,
                    renderer: 'canvas',
                    loop: false,
                    autoplay: false,
                    animationData: script
                })
                let canvas = root.firstElementChild
                let duration = animation.getDuration()
                let fr = script['fr']
                if (!fps) fps = fr
                if (gif_worker == null) {
                    gif_worker = window.URL.createObjectURL(await (await 
                        fetch("https://cdn.jsdelivr.net/npm/gif.js@0.2.0/dist/gif.worker.js")).blob());
                }
                let gif = new GIF({
                    workers: 4,
                    quality: 30,
                    workerScript: gif_worker,
                    transparent: "#000000"
                })
                for (let frame = 0; frame < duration * fps; frame++) {
                    animation.goToAndStop(Math.floor(frame * fr / fps), true)
                    await wait(5)
                    gif.addFrame(canvas, { delay: 1000 / fps, copy: true })
                }
                let res = await renderGif(gif)
                animation.destroy()
                root.remove()
                return res
            }
            </script>
            </body>
            </html>
        """.trimIndent())
        page.setViewport(
            Viewport(5000, 5000, 1,
                false, false, false)
        )
        page
    }

    suspend fun renderLottie(script: String) : ByteArray {
        lottieMutex.lock()
        lottieMutex.unlock()
        return lottiePage.evaluate("""(async () => await genGif($script, 30, 0.5))""").toString().let {
            Base64.getDecoder().decode(it.removePrefix("data:image/gif;base64,"))!!
        }
    }

    suspend fun renderTweet(url : String) : ByteArray {
        println("Render Tweet: $url")
        val page = browser.newPage()!!
        page.setViewport(
            Viewport(1080, 5000, 1.5,
            false, false, false)
        )
        println("Page opened")
        doIO { page.goTo(url) }
        println("Tweet Loading")
        delay(5000)
        println("Tweet Loaded")
        val tweet = doIO {
            page.waitForFunction("""
                (() => 
                    Array.from(document.querySelectorAll("article")).find(e => 
                        typeof(e.attributes.tabindex) == 'undefined')
                )
            """)
        }?.asElement() ?: throw Throwable("not tweet")
        val image = Base64.getDecoder().decode(doIO { tweet.screenshot(jpgShot) })!!
        doIO { page.close() }
        println("Tweet Rendered")
        return image
    }
}

suspend fun main() {
    headless = true
    WebPage.renderTex("test")
    File("tex.png").writeBytes(
        printTime("Total") {
            WebPage.renderTex("test")
        }
    )
    println("Done.")
}