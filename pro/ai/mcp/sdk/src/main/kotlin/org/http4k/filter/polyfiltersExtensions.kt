package org.http4k.filter

import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.sse.SseMessage
import org.http4k.sse.then
import java.io.PrintStream

fun PolyFilters.DebugMcp(out: PrintStream = System.out) = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let { DebuggingFilters.PrintRequestAndResponse(out = out).then(it) },
        sse = next.sse?.let {
            DebuggingFilters.PrintSseRequest(out, true)
                .then(DebuggingFilters.PrintSseResponse(out, notAPing)).then(it)
        },
    )
}

fun PolyHandler.debugMcp(out: PrintStream = System.out) = PolyFilters.DebugMcp(out).then(this)

private val notAPing: (SseMessage) -> Boolean = { it is SseMessage.Event && it.event != "ping" }
