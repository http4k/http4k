/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.testing.patched

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseHandler
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.chunkedSseSequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class PolyHandlerToHttpHandlerTest {
    private val closed = AtomicBoolean(false)
    private val connection = AtomicReference<Sse>()

    private val live: SseHandler = {
        SseResponse { sse ->
            connection.set(sse)
            sse.send(SseMessage.Data("hello"))
            sse.onClose { closed.set(true) }
        }
    }

    private val app = PolyHandler(
        http = routes(
            "/live" bind GET to { Response(OK).body("first paint") },
            "/live/join" bind POST to { Response(OK).body("joined") },
        ),
        sse = sse("/live" bind live),
    ).toHttpHandler()

    private fun Request.acceptingEvents() = header("Accept", "text/event-stream, text/html")

    @Test
    fun `an event-stream request to an sse route streams its messages over http`() {
        val response = app(Request(GET, "/live").acceptingEvents())
        val events = response.body.stream.chunkedSseSequence().iterator()

        assertEquals(TEXT_EVENT_STREAM.value, CONTENT_TYPE(response)?.value)
        assertEquals(SseMessage.Data("hello"), events.next())

        connection.get().send(SseMessage.Data("later"))

        assertEquals(SseMessage.Data("later"), events.next())
    }

    @Test
    fun `an event-stream request that no sse route handles falls through to http, as Jetty does`() {
        assertEquals("joined", app(Request(POST, "/live/join").acceptingEvents()).bodyString())
    }

    @Test
    fun `a plain request is served by http`() {
        assertEquals("first paint", app(Request(GET, "/live")).bodyString())
    }

    @Test
    fun `closing the http stream closes the connection`() {
        val events = app(Request(GET, "/live").acceptingEvents()).body.stream
        events.chunkedSseSequence().first()

        events.close()

        assertEquals(true, closed.get())
    }
}
