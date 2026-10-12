package org.http4k.sse

import org.http4k.core.ContentEncoding
import org.http4k.core.Headers
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK

interface SseResponse : SseConsumer {
    val status: Status
    val headers: Headers
    val handled: Boolean
    val consumer: SseConsumer

    /**
     * The encoding the server should compress the event stream with, if it is able to.
     */
    val contentEncoding: ContentEncoding? get() = null

    fun withConsumer(consumer: SseConsumer): SseResponse

    /**
     * Defaults to ignoring the encoding, so implementations (and wrappers) must override this to support compression.
     */
    fun withContentEncoding(encoding: ContentEncoding): SseResponse = this

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
    override val consumer: SseConsumer,
    override val contentEncoding: ContentEncoding? = null
) : SseResponse, SseConsumer by consumer {
    override fun withConsumer(consumer: SseConsumer): SseResponse = copy(consumer = consumer)
    override fun withContentEncoding(encoding: ContentEncoding): SseResponse = copy(contentEncoding = encoding)
}
