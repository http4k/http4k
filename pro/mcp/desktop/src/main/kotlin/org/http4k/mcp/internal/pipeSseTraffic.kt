package org.http4k.mcp.internal

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.readLines
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.lens.accept
import org.http4k.lens.contentType
import org.http4k.sse.SseClient
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import java.io.Reader
import java.io.Writer

/**
 * Connect to the SSE, constructing the request using the passed function
 */
fun pipeSseTraffic(
    input: Reader,
    output: Writer,
    scheduler: SimpleScheduler,
    sseRequest: Request,
    http: HttpHandler,
    makeSseClient: (Request) -> SseClient,
) {
    val httpWithHost = SetHostFrom(sseRequest.uri).then(http)
    makeSseClient(sseRequest.accept(TEXT_EVENT_STREAM))
        .use {
            it.received()
                .forEach { msg ->
                    with(output) {
                        when (msg) {
                            is Event -> when (msg.event) {
                                "endpoint" -> scheduler.readLines(input) {
                                    httpWithHost(
                                        Request(POST, msg.data)
                                            .contentType(APPLICATION_JSON)
                                            .body(it)
                                    )
                                }

                                else -> write("${msg.data}\n")
                            }

                            is Data -> write("${msg.data}\n")
                            else -> {}
                        }
                        flush()
                    }
                }
        }
}
