/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.authentication

import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.lens.RequestKey
import org.http4k.lens.location
import org.http4k.routing.bind
import org.http4k.routing.poly
import org.http4k.sse.SseResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthenticatedTest {
    private val principal = RequestKey.required<String>("principal")
    private val streamedBy = mutableListOf<String>()

    private val app = Authenticated(principal) { it.header("who") }.then(
        poly(
            "/things/{id}" bind PolyHandler(
                http = { Response(OK).body(principal(it)) },
                sse = { request ->
                    streamedBy += principal(request)
                    SseResponse { it.close() }
                },
            ),
        ),
    )

    private val pages = app.http!!
    private val streams = app.sse!!
    private val thing = Request(GET, "/things/3")

    @Test
    fun `a page knows who is asking`() {
        assertEquals("ann", pages(thing.header("who", "ann")).bodyString())
    }

    @Test
    fun `a page refuses someone it can't identify`() {
        assertEquals(UNAUTHORIZED, pages(thing).status)
    }

    @Test
    fun `a stream knows who is asking`() {
        assertEquals(OK, streams(thing.header("who", "ann")).status)
        assertEquals(listOf("ann"), streamedBy)
    }

    @Test
    fun `a stream refuses someone it can't identify, and never starts`() {
        assertEquals(UNAUTHORIZED, streams(thing).status)
        assertEquals(emptyList<String>(), streamedBy)
    }

    @Test
    fun `a refused page can be sent somewhere else, while a refused stream stays a 401`() {
        val app = Authenticated(principal, refused = { Response(SEE_OTHER).location(Uri.of("/login")) }) { null }.then(
            poly("/things/{id}" bind PolyHandler(http = { Response(OK) }, sse = { SseResponse { it.close() } })),
        )

        val page = app.http!!(thing)

        assertEquals(SEE_OTHER, page.status)
        assertEquals("/login", page.header("location"))
        assertEquals(UNAUTHORIZED, app.sse!!(thing).status)
    }

    @Test
    fun `routes keep matching behind it`() {
        assertEquals(NOT_FOUND, pages(Request(GET, "/nowhere").header("who", "ann")).status)
    }
}
