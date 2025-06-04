package org.http4k.filter

import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.sse.SseMessage
import org.http4k.sse.then
import java.io.PrintStream

fun PolyHandler.debugMcp(out: PrintStream = System.out) = DebuggingFilters.PrintSseRequest(out, true)
    .then(DebuggingFilters.PrintSseResponse(out, notAPing))
    .then(this)

private val notAPing: (SseMessage) -> Boolean = { it is SseMessage.Event && it.event != "ping" }
