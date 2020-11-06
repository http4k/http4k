package org.http4k.routing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class RequestMatchRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler = validPath bind GET to routes(headers("host") bind { Response(OK) })

    @Test
    fun `path element still recoverable`() {
        val criteria = present(hasStatus(OK).and(hasBody("somevalue")))

        val handler = routes("/{foobar}" bind GET to routes(headers("host") bind {
            Response(OK).body(it.path("foobar") ?: "")
        }))

        val req = Request(GET, "/somevalue").header("host", "host")
        assertThat(handler.matchAndInvoke(req), criteria)
        assertThat(handler(req), criteria)

        val prefixReq = Request(GET, "/prefix/somevalue").header("host", "host")
        val withBasePath = handler.withBasePath(prefix)
        assertThat(withBasePath.matchAndInvoke(prefixReq), criteria)
        assertThat(withBasePath(prefixReq), criteria)
    }

    @Test
    fun `attempt to bind param handler without a verb - without header`() {
        val app = routes(prefix bind (headers("host") bind { Response(OK) }))
        assertThat(app(Request(GET, "")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `attempt to bind param handler without a verb - with header`() {
        val app = routes(prefix bind (headers("host") bind { Response(OK) }))
        assertThat(app(Request(GET, "").header("host", "foo")), hasStatus(OK))
    }
}

class RequestMatchRoutingHttpHandlerAlternateTest : RoutingHttpHandlerContract() {
    override val handler = headers("host") bind routes(validPath bind GET to { Response(OK) })
}

