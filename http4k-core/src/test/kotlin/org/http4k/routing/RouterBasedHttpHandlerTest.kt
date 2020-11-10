package org.http4k.routing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class RouterBasedHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler = validPath bind GET to routes(headers("host") bind { Response(OK) })
}

class RouterBasedHttpHandlerReverseTest : RoutingHttpHandlerContract() {
    override val handler = headers("host") bind routes(validPath bind GET to { Response(OK) })
}

class RouterBasedHttpHandlerSpecialCaseTests {
    private val prefix = "/prefix"

    @Test
    fun `multi param routes - verb first`() {
        val handler = routes(GET to routes("/{foo}" bind
            routes("/{bar}" bind { it: Request ->
                Response(OK).body(it.path("foo")!! + " then " + it.path("bar"))
            })
        ))

        assertThat(handler(Request(GET, "/one/two")), hasStatus(OK).and(hasBody("one then two")))
    }

    @Test
    fun `multi param routes - verb middle`() {
        val handler = routes("/{foo}" bind GET to routes(
            "/{bar}" bind { r: Request -> Response(OK).body(r.path("foo")!! + " then " + r.path("bar")) }
        ))

        assertThat(handler(Request(GET, "/one/two")), hasStatus(OK).and(hasBody("one then two")))
    }

    @Test
    fun `multi param routes - verb second`() {
        val handler = routes("/{foo}" bind routes(
            "/{bar}" bind GET to { Response(OK).body(it.path("foo")!! + " then " + it.path("bar")) },
        ))

        assertThat(handler(Request(GET, "/one/two")), hasStatus(OK).and(hasBody("one then two")))
    }

    @Test
    fun `router + passthrough with filter`(){
        val filter = Filter { next ->
            {
                next(it.body(it.query("foo").orEmpty()))
            }
        }

        val handler = filter.then(routes(queries("foo") bind { Response(OK).body(it.body) }))

        assertThat(handler(Request(GET, "").query("foo", "bar")), hasStatus(OK).and(hasBody("bar")))
    }

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
        assertThat(app(Request(GET, prefix)), hasStatus(NOT_FOUND))
    }

    @Test
    fun `attempt to bind param handler without a verb - wrong route`() {
        val app = routes(prefix bind (headers("host") bind { Response(OK) }))
        val header = Request(GET, "/unknown").header("host", "foo")
        assertThat(app(header), hasStatus(NOT_FOUND))
    }

    @Test
    fun `attempt to bind param handler without a verb - with header`() {
        val app = routes(prefix bind (headers("host") bind { Response(OK) }))
        assertThat(app(Request(GET, prefix).header("host", "foo")), hasStatus(OK))
    }

    @Test
    fun `attempt to bind fallback handler without a verb - wrong route`() {
        val app = routes(prefix bind routes(
            header("host", "host1") bind { Response(OK) },
            header("host", "host2") bind { Response(INTERNAL_SERVER_ERROR) }
        ))
        assertThat(app(Request(GET, prefix).header("host", "host1")), hasStatus(OK))
        assertThat(app(Request(GET, prefix).header("host", "host2")), hasStatus(INTERNAL_SERVER_ERROR))
        assertThat(app(Request(GET, prefix).header("host", "foo")), hasStatus(NOT_FOUND))
        assertThat(app(Request(GET, prefix)), hasStatus(NOT_FOUND))
    }
}
