package org.http4k.sse

import org.http4k.core.Request
import org.http4k.routing.RoutingSseHandler

interface Sse : AutoCloseable {
    val connectRequest: Request
    fun send(message: SseMessage): Sse
    override fun close()
    fun onClose(fn: () -> Unit): Sse
}

typealias SseConsumer = (Sse) -> Unit

typealias SseHandler = (Request) -> SseResponse

fun interface SseFilter : (SseHandler) -> SseHandler {
    companion object
}

val SseFilter.Companion.NoOp: SseFilter get() = SseFilter { next -> { next(it) } }

fun SseFilter.then(next: SseFilter): SseFilter = SseFilter { this(next(it)) }

fun SseFilter.then(next: SseHandler): SseHandler = { this(next)(it) }

fun SseFilter.then(routingSseHandler: RoutingSseHandler): RoutingSseHandler = routingSseHandler.withFilter(this)

