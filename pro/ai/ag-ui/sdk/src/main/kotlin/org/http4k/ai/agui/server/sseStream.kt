/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.server

import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.sse.SseMessage
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

/**
 * Encode a stream of AG-UI events as an SSE byte stream. Each event becomes one
 * `data: {json}\n\n` SSE frame, using [AgUiJson] for polymorphic serialisation.
 */
internal fun Sequence<AgUiEvent>.toSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            for (event in this) {
                val json = AgUiJson.asJsonString(event, AgUiEvent::class)
                out.write(SseMessage.Data(json).toMessage().toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}
