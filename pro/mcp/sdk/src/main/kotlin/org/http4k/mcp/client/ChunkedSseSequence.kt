package org.http4k.mcp.client

import org.http4k.sse.SseMessage
import java.io.InputStream

fun InputStream.chunkedSseSequence(): Sequence<SseMessage> = sequence {
    use {
        val buffer = StringBuilder()
        var lastChar: Int = -1
        var newlineCount = 0

        while (true) {
            val current = it.read()
            if (current == -1) {
                if (buffer.isNotEmpty()) {
                    yield(SseMessage.parse(buffer.toString()))
                }
                break
            }

            val currentChar = current.toChar()
            buffer.append(currentChar)

            if (currentChar == '\n') {
                if (lastChar == '\n'.code) {
                    newlineCount++
                    if (newlineCount == 2) {
                        yield(SseMessage.parse(buffer.substring(0, buffer.length - 2)))
                        buffer.clear()
                        newlineCount = 0
                    }
                } else {
                    newlineCount = 1
                }
            } else {
                newlineCount = 0
            }

            lastChar = current
        }
    }
}
