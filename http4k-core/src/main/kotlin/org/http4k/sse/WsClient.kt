package org.http4k.sse

import org.http4k.sse.SseStatus.Companion.NORMAL

interface SseClient {
    fun received(): Sequence<SseMessage>
    fun close(status: SseStatus = NORMAL)
    fun send(message: SseMessage)
}
