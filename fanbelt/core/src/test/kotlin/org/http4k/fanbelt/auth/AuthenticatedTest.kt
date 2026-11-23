package org.http4k.fanbelt.auth

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
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
    fun `a page or stream knows who is asking`() {
        assertThat(pages(thing.header("who", "ann")).bodyString(), equalTo("ann"))
        assertThat(streams(thing.header("who", "ann")).status, equalTo(OK))
        assertThat(streamedBy, equalTo(listOf("ann")))
    }

    @Test
    fun `a page or stream refuses someone it can't identify, and the stream never starts`() {
        assertThat(pages(thing).status, equalTo(UNAUTHORIZED))
        val refused = streams(thing)

        assertThat(refused.status, equalTo(UNAUTHORIZED))
        assertThat(refused.handled, equalTo(true))
        assertThat(streamedBy, equalTo(emptyList<String>()))
    }

    @Test
    fun `a refused page or stream can be sent somewhere else`() {
        val app = Authenticated(principal, onRefused = { Response(SEE_OTHER).location(Uri.of("/login")) }) { null }.then(
            poly("/things/{id}" bind PolyHandler(http = { Response(OK) }, sse = { SseResponse { it.close() } })),
        )

        val page = app.http!!(thing)

        assertThat(page.status, equalTo(SEE_OTHER))
        assertThat(page.header("location"), equalTo("/login"))
        val stream = app.sse!!(thing)

        assertThat(stream.status, equalTo(SEE_OTHER))
        assertThat(stream.headers.toMap()["location"], equalTo("/login"))
    }
}
