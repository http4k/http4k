package org.http4k.sse

interface SseClient {
    fun received(): Sequence<SseMessage>
    fun close()
    fun send(message: SseMessage)
}
