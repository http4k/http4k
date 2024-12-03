package org.http4k.sse

import org.http4k.core.Headers
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK

interface SseResponse {
    val status: Status
    val headers: Headers
    val handled: Boolean
    val consumer: SseConsumer

    fun consumer(consumer: SseConsumer): SseResponse

    companion object {
        operator fun invoke(consumer: SseConsumer): SseResponse =
            MemorySseResponse(OK, emptyList(), true, consumer)

        operator fun invoke(
            status: Status = OK, headers: Headers = emptyList(), handled: Boolean = true, consumer: SseConsumer
        ): SseResponse = MemorySseResponse(status, headers, handled, consumer)
    }
}

internal data class MemorySseResponse(
    override val status: Status = OK,
    override val headers: Headers = emptyList(),
    override val handled: Boolean = true,
    override val consumer: SseConsumer
) : SseResponse, SseConsumer by consumer {
    override fun consumer(consumer: SseConsumer): SseResponse = copy(consumer = consumer)
}
