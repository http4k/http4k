/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import java.io.IOException
import java.io.OutputStream

/**
 * An [Sse] that serialises each message as `text/event-stream` frames straight into [output] (a
 * [java.io.PipedOutputStream] whose connected input stream is the HTTP response body). Lets the non-streaming
 * HTTP face stream a handler's progress/log notifications + terminal result as an ordinary streaming response
 * body — no SSE server-adapter needed, so it works identically on every adapter. A broken pipe (client gone)
 * surfaces as [IOException] and closes the stream, unblocking the producing virtual thread.
 */
internal class PipedSse(private val output: OutputStream, override val connectRequest: Request) : Sse {
    private val onClose = mutableListOf<() -> Unit>()

    override fun send(message: SseMessage) = apply {
        try {
            output.write(message.toMessage().toByteArray())
            output.flush()
        } catch (e: IOException) {
            close()
        }
    }

    override fun close() {
        onClose.forEach { it() }
        runCatching { output.close() }
    }

    override fun onClose(fn: () -> Unit) = apply { onClose += fn }
}
