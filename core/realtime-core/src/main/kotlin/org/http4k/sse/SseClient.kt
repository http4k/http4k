package org.http4k.sse

interface SseClient : AutoCloseable {
    fun received(): Sequence<SseMessage>
    override fun close()
}
