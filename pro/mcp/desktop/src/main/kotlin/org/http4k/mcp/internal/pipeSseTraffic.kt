package org.http4k.mcp.internal

import org.http4k.client.Http4kSseClient
import org.http4k.client.ReconnectionMode
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.lens.accept
import org.http4k.lens.contentType
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

/**
 * Connect to the SSE, constructing the request using the passed function
 */
fun pipeSseTraffic(
    input: Reader,
    output: Writer,
    sseRequest: Request,
    http: HttpHandler,
    reconnectionMode: ReconnectionMode
) {
    val incomingMessages = input.buffered().lineSequence()

    val httpWithHost = SetHostFrom(sseRequest.uri).then(http)
    thread {
        Http4kSseClient(sseRequest.accept(TEXT_EVENT_STREAM), http, reconnectionMode).received().forEach { msg ->
            when (msg) {
                is Event -> when (msg.event) {
                    "endpoint" -> {
                        thread {
                            incomingMessages
                                .forEach {
                                    require(
                                        httpWithHost(
                                            Request(POST, msg.data)
                                                .contentType(APPLICATION_JSON)
                                                .body(it)
                                        ).status.successful
                                    )
                                }
                        }
                    }

                    "ping" -> {}

                    else -> output.write("${msg.data}\n")
                }

                is Data -> output.write("${msg.data}\n")
                else -> {}
            }
            output.flush()
        }
    }
}
