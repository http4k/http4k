package org.http4k.sse

import org.http4k.core.Request
import java.io.InputStream
import java.time.Duration
import java.util.Base64.getEncoder

interface Sse {
    val connectRequest: Request
    fun send(message: SseMessage)
    fun close()
    fun onClose(fn: () -> Unit)
}

typealias SseConsumer = (Sse) -> Unit

typealias SseHandler = (Request) -> SseConsumer?

sealed class SseMessage {
    data class Data(val data: String) : SseMessage() {
        constructor(data: ByteArray) : this(getEncoder().encodeToString(data))
        constructor(data: InputStream) : this(data.readAllBytes())
    }

    data class Event(val event: String, val data: String, val id: String? = null) : SseMessage() {
        constructor(event: String, data: ByteArray, id: String? = null) : this(
            event,
            getEncoder().encodeToString(data),
            id
        )

        constructor(event: String, data: InputStream, id: String? = null) : this(event, data.readAllBytes(), id)
    }

    data class Retry(val backoff: Duration) : SseMessage()

    companion object
}
