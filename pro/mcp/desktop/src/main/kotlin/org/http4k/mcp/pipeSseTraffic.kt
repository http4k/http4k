package org.http4k.mcp

import org.http4k.client.JavaSseClient
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.accept
import org.http4k.sse.SseClient
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import java.io.InputStreamReader
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

/**
 * Simply connect to the SSE using the passed URL
 */
fun pipeSseTraffic(serverUrl: Uri, input: Reader = InputStreamReader(System.`in`), output: Writer = System.out.writer()) =
    pipeSseTraffic(input, output) { Request(POST, serverUrl) }

/**
 * Connect to the SSE, constructing the request using the passed function
 */
fun pipeSseTraffic(
    input: Reader = InputStreamReader(System.`in`),
    output: Writer = System.out.writer(),
    sseClient: (Request) -> SseClient = JavaSseClient(),
    baseRequest: RequestProvider
) {
    input.useLines {
        it.forEach {
            thread {
                sseClient(baseRequest()
                    .accept(TEXT_EVENT_STREAM).body(it)).use {
                    it.received().forEach {
                        with(output) {
                            when (it) {
                                is Data -> write("${it.data}\n")
                                is Event -> write("${it.data}\n")
                                else -> {}
                            }
                            flush()
                        }
                    }
                }
            }
        }
    }
}
