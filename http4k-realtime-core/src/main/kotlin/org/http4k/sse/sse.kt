package org.http4k.sse

import org.http4k.base64Encode
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.routing.RoutingSseHandler
import java.io.InputStream
import java.time.Duration

interface Sse {
    fun send(message: SseMessage)
    fun close()
    fun onClose(fn: () -> Unit)
}

typealias SseConsumer = (Sse) -> Unit

data class SseResponse(val headers: Headers = emptyList(), val consumer: SseConsumer) {
    constructor(consumer: SseConsumer) : this(emptyList(), consumer)
}

typealias SseHandler = (Request) -> SseResponse

sealed class SseMessage {
    data class Data(val data: String) : SseMessage() {
        constructor(data: ByteArray) : this(data.base64Encode())
        constructor(data: InputStream) : this(data.readAllBytes())
    }

    data class Event(val event: String, val data: String, val id: String? = null) : SseMessage() {
        constructor(event: String, data: ByteArray, id: String? = null) : this(
            event,
            data.base64Encode(),
            id
        )

        constructor(event: String, data: InputStream, id: String? = null) : this(event, data.readAllBytes(), id)
    }

    data class Retry(val backoff: Duration) : SseMessage()

    companion object
}

fun interface SseFilter : (SseHandler) -> SseHandler {
    companion object
}

val SseFilter.Companion.NoOp: SseFilter get() = SseFilter { next -> { next(it) } }

fun SseFilter.then(next: SseFilter): SseFilter = SseFilter { this(next(it)) }

fun SseFilter.then(next: SseHandler): SseHandler = { this(next)(it) }

fun SseFilter.then(routingSseHandler: RoutingSseHandler): RoutingSseHandler = routingSseHandler.withFilter(this)
