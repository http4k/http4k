package merge_signals

import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.format.Moshi
import org.http4k.format.asDatastarSignal
import org.http4k.routing.poly
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.routing.static
import org.http4k.sse.Sse
import org.http4k.sse.sendMergeSignals
import java.io.File
import java.io.InputStream
import java.lang.Thread.sleep
import java.lang.Thread.startVirtualThread
import java.util.zip.ZipFile

fun badApples(): PolyHandler {
    val resourceName = "/merge_signals/bad-apples.zip"
    val resourceStream: InputStream = object {}.javaClass.getResourceAsStream(resourceName)
        ?: throw IllegalStateException("Resource not found: $resourceName")
    
    val tempFile = File.createTempFile("bad-apples", ".zip")
    tempFile.deleteOnExit()
    resourceStream.use { input ->
        tempFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    
    val animation = loadAnimation(tempFile)

    return poly(
        "/bad_apple/updates" bind sse(
            GET to sse {
                var currentFrameIdx = 0

                startVirtualThread {
                    while (true) {
                        when {
                            currentFrameIdx >= animation.frames.size -> break
                            else -> currentFrameIdx = it.sendNextFrame(animation, currentFrameIdx)
                        }
                        sleep(33)
                    }
                }
            }
        ),
        static()
    )
}

data class BadAppleStore(val _contents: String, val percentage: Double)

private fun Sse.sendNextFrame(animation: AsciiAnimation, currentFrameIdx: Int): Int {
    val frame = animation.frames[currentFrameIdx]
    val percentage = 100.0 * (currentFrameIdx + 1) / animation.frames.size

    sendMergeSignals(Moshi.asDatastarSignal(BadAppleStore(frame, percentage)))

    return currentFrameIdx + 1
}

class AsciiAnimation(val frames: List<String>)

private fun loadAnimation(zipFile: File): AsciiAnimation {
    val frames = mutableListOf<String>()

    ZipFile(zipFile).use {
        it.entries().asSequence()
            .sortedBy { it.name }
            .forEach { entry ->
                it.getInputStream(entry).use { inputStream ->
                    frames.add(inputStream.bufferedReader().use { it.readText() })
                }
            }
    }

    return AsciiAnimation(frames)
}

