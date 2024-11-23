package org.http4k.filter


import org.http4k.core.HttpMessage
import org.http4k.core.MemoryBody
import org.http4k.sse.Sse
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseMessage.Data
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseMessage.Retry
import org.http4k.sse.then
import java.io.PrintStream

fun DebuggingFilters.PrintSseRequest(out: PrintStream = System.out, debugStream: Boolean = false) =
    SseFilter { next ->
        { req ->
            out.println(
                listOf(
                    "***** SSE REQUEST: ${req.method}: ${req.uri} *****",
                    req.printable(debugStream)
                ).joinToString("\n")
            )
            next(req)
        }
    }

fun DebuggingFilters.PrintSseRequestAndResponse(out: PrintStream = System.out, debugStream: Boolean = false) =
    PrintSseRequest(out, debugStream).then(PrintSseResponse(out))

fun SseHandler.debug(out: PrintStream = System.out, debugStream: Boolean = false) =
    DebuggingFilters.PrintSseRequestAndResponse(out, debugStream).then(this)

fun DebuggingFilters.PrintSseResponse(out: PrintStream = System.out) =
    SseFilter { next ->
        { req ->
            try {
                next(req).let { response ->
                    out.println(
                        (
                            listOf(
                                "***** SSE RESPONSE ${response.status.code} to ${req.method}: ${req.uri} *****"
                            ) +
                                response.headers.map { "${it.first}: ${it.second}" }
                            )
                            .joinToString("\n")
                    )

                    response.copy(consumer = { sse ->
                        response.consumer(object : Sse by sse {
                            override fun send(message: SseMessage) {
                                sse.send(message)
                                out.println(
                                    "***** SSE SEND ${req.method}: ${req.uri} -> " + when (message) {
                                        is Data -> "Data: ${message.data}"
                                        is Event -> "Event: ${message.event} ${message.data} ${message.id ?: ""}"
                                        is Retry -> "Retry: ${message.backoff}"
                                    }
                                )
                            }

                            override fun close() {
                                sse.close()
                                out.println("***** SSE CLOSED on ${req.method}: ${req.uri} *****")
                            }
                        })
                    })
                }
            } catch (e: Exception) {
                out.println("***** SSE RESPONSE FAILED to ${req.method}: ${req.uri} *****")
                e.printStackTrace(out)
                throw e
            }
        }
    }

private fun HttpMessage.printable(debugStream: Boolean) =
    if (debugStream || body is MemoryBody) this else body("<<stream>>")

