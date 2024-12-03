package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.MemoryBody
import org.http4k.core.RequestContext
import org.http4k.core.SseTransaction
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Store
import org.http4k.routing.sse.RoutingSseHandler
import org.http4k.sse.Sse
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import java.io.PrintStream
import java.time.Clock
import java.time.Duration
import java.time.Instant

fun ServerFilters.CatchAllSse(
    onError: (Throwable) -> SseResponse = ::originalSseBehaviour,
) = SseFilter { next ->
    {
        try {
            next(it)
        } catch (e: Throwable) {
            onError(e)
        }
    }
}

private fun originalSseBehaviour(e: Throwable): SseResponse {
    if (e !is Exception) throw e
    e.printStackTrace()
    return SseResponse(INTERNAL_SERVER_ERROR) { it.close() }
}

fun ServerFilters.InitialiseSseRequestContext(contexts: Store<RequestContext>) = SseFilter { next ->
    {
        val context = RequestContext()
        try {
            next(contexts(context, it))
        } finally {
            contexts.remove(context)
        }
    }
}

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

fun RoutingSseHandler.debug(out: PrintStream = System.out, debugStream: Boolean = false) =
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

                    response.withConsumer { sse ->
                        response.consumer(object : Sse by sse {
                            override fun send(message: SseMessage) = apply {
                                sse.send(message)
                                out.println("""***** SSE SEND ${req.method}: ${req.uri} -> ${message::class.simpleName}""")
                                out.println(message.toMessage())
                            }

                            override fun close() {
                                sse.close()
                                out.println("***** SSE CLOSED on ${req.method}: ${req.uri} *****")
                            }
                        })
                    }
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

/**
 * General reporting Filter for an ReportHttpTransaction. Pass an optional HttpTransactionLabeler to
 * create custom labels.
 * This is useful for logging metrics. Note that the passed function blocks the response from completing.
 */
fun ResponseFilters.ReportSseTransaction(
    clock: Clock = Clock.systemUTC(),
    transactionLabeler: SseTransactionLabeler = { it },
    recordFn: (SseTransaction) -> Unit
): SseFilter = ReportSseTransaction(clock::instant, transactionLabeler, recordFn)

/**
 * General reporting SseFilter for an ReportSseTransaction. Pass an optional SseTransactionLabeler to
 * create custom labels.
 * This is useful for logging metrics. Note that the passed function blocks the response from completing.
 */
fun ResponseFilters.ReportSseTransaction(
    timeSource: () -> Instant,
    transactionLabeler: SseTransactionLabeler = { it },
    recordFn: (SseTransaction) -> Unit
) = SseFilter { next ->
    { request ->
        timeSource().let { start ->
            next(request).let { response ->
                response.withConsumer { sse ->
                    response.consumer(object : Sse by sse {
                        override fun close() {
                            sse.close()
                            recordFn(
                                transactionLabeler(
                                    SseTransaction(
                                        request = request,
                                        response = response,
                                        start = start,
                                        duration = Duration.between(start, timeSource())
                                    )
                                )
                            )
                        }
                    })
                }
            }
        }
    }
}

typealias SseTransactionLabeler = (SseTransaction) -> SseTransaction
