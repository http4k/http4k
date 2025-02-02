package org.http4k.mcp.client.internal

import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Event
import java.io.InputStream

internal inline fun <reified T : Any> Event.asAOrThrow() = with(McpJson) {
    val result = JsonRpcResult(this, (parse(data) as MoshiObject).attributes)
    when {
        result.isError() -> error("Failed: " + asFormatString(result.error ?: nullNode()))
        else -> asA<T>(compact(result.result ?: nullNode()))
    }
}


internal fun InputStream.chunkedSseSequence(): Sequence<SseMessage> = sequence {
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
