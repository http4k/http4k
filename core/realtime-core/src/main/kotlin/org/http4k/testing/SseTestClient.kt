package org.http4k.testing

import org.http4k.sse.SseMessage

interface SseTestClient : AutoCloseable {
    fun received(): Sequence<SseMessage>
    override fun close()
}
