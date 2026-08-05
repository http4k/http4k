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
import java.util.concurrent.atomic.AtomicBoolean

internal class PipedSse(
    private val output: OutputStream,
    override val connectRequest: Request,
    private val onFirstSend: () -> Unit = {},
) : Sse {
    private val onClose = mutableListOf<() -> Unit>()
    private val firstSend = AtomicBoolean(true)

    override fun send(message: SseMessage) = apply {
        if (firstSend.compareAndSet(true, false)) onFirstSend()
        try {
            output.write(message.toMessage().toByteArray())
            output.flush()
        } catch (_: IOException) {
            close()
        }
    }

    override fun close() {
        onClose.forEach { it() }
        runCatching { output.close() }
    }

    override fun onClose(fn: () -> Unit) = apply { onClose += fn }
}
