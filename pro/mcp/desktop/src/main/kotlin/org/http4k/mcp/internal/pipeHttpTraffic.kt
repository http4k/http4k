package org.http4k.mcp.internal

import org.http4k.client.chunkedSseSequence
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.lens.accept
import org.http4k.lens.contentType
import org.http4k.sse.SseMessage.Event
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

/**
 * Connect to the HTTP endpoint, constructing the request using the passed function
 */
fun pipeHttpTraffic(input: Reader, output: Writer, sseRequest: Request, http: HttpHandler) {
    input.buffered().lineSequence().forEach { next ->
        thread {
            val response = http(
                Request(POST, sseRequest.uri)
                    .accept(TEXT_EVENT_STREAM)
                    .contentType(APPLICATION_JSON)
                    .body(next)
            )
            if (response.bodyString().isNotEmpty()) {
                response.bodyString().byteInputStream().chunkedSseSequence()
                    .filterIsInstance<Event>()
                    .forEach { output.apply { write("${it.data}\n") }.flush() }
            }
        }
    }
}
