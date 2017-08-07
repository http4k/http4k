package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

class RoutingTest {

    @Test
    fun `not found`() {
        val routes = routes()

        val response = routes(Request(GET, "/a/something"))

        assertThat(response.status, equalTo(NOT_FOUND))
    }

    @Test
    @Ignore
    fun `method not allowed`() {
        val routes = routes(
            "/a/{route}" to GET bind { Response(OK).body("matched") }
        )

        val response = routes(Request(POST, "/a/something"))

        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun `matches uri template and method`() {
        val routes = routes(
            "/a/{route}" to GET bind { Response(OK).body("matched") }
        )

        val response = routes(Request(GET, "/a/something"))

        assertThat(response.bodyString(), equalTo("matched"))
    }

    @Test
    fun `can mix and match Route styles`() {
        val routes = routes(
            "/a" to GET bind { Response(OK).body("matched a") },
            "/b/c" bind routes(
                "/d" to GET bind { Response(OK).body("matched b/c/d") },
                "/e" bind routes(
                    "/f" to GET bind { Response(OK).body("matched b/c/e/f") }
                ),
                "/" to GET bind { Response(OK).body("matched b/c") }
            )
        )

        assertThat(routes(Request(GET, "/a")).bodyString(), equalTo("matched a"))
        assertThat(routes(Request(GET, "/b/c/d")).bodyString(), equalTo("matched b/c/d"))
        assertThat(routes(Request(GET, "/b/c")).bodyString(), equalTo("matched b/c"))
        assertThat(routes(Request(GET, "/b/c/e/f")).bodyString(), equalTo("matched b/c/e/f"))
        assertThat(routes(Request(GET, "/b/c/e/g")).status, equalTo(NOT_FOUND))
        assertThat(routes(Request(GET, "/b")).status, equalTo(NOT_FOUND))
        assertThat(routes(Request(GET, "/b/e")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `matches uses first match`() {
        val routes = routes(
            "/a/{route}" to GET bind { Response(OK).body("matched a") },
            "/a/{route}" to GET bind { Response(OK).body("matched b") }
        )

        val response = routes(Request(GET, "/a/something"))

        assertThat(response.bodyString(), equalTo("matched a"))
    }

    @Test
    fun `path parameters are available in request`() {
        val routes = routes(
            "/{a}/{b}/{c}" to GET bind { req: Request -> Response(OK).body("matched ${req.path("a")}, ${req.path("b")}, ${req.path("c")}") }
        )

        val response = routes(Request(GET, "/x/y/z"))

        assertThat(response.bodyString(), equalTo("matched x, y, z"))
    }

    @Test
    fun `matches uri with query`() {
        val routes = routes("/a/b" to GET bind { Response(OK) })

        val response = routes(Request(GET, "/a/b?foo=bar"))

        assertThat(response, equalTo(Response(OK)))
    }

    @Test
    fun `matches regex uri with query`() {
        val handler = routes("/a/{b:.+}" to GET bind { Response(OK).body(it.path("b")!!) })
        assertThat(handler(Request(GET, "/a/foo?bob=rita")).bodyString(), equalTo("foo"))
    }

    @Test
    fun `matches request with extra path parts`() {
        val routes = routes("/a" to GET bind { Response(OK) })

        val response = routes(Request(GET, "/a/b"))

        assertThat(response, equalTo(Response(OK)))
    }

    @Test
    fun `can stop matching extra parts`() {
        val routes = routes("/a{$}" to GET bind { Response(OK) })

        val response = routes(Request(GET, "/a/b"))

        assertThat(response, equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun `breaks if trying to access path parameters without header present`() {
        try {
            Request(GET, "/").path("abc")
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertThat(e.message, equalTo("x-uri-template header not present in the request"))
        }
    }

    @Test
    fun `can put routes inside of routes`() {
        val subRoutes = routes(
            "/a/{route}" to GET bind { Response(OK).header("header", it.header("header")).body("matched") }
        )

        var count = 0
        val filter = Filter { next ->
            {
                next(it.replaceHeader("header", "value" + count++))
            }
        }

        val app = routes("/prefix" bind filter.then(subRoutes))

        val response = app(Request(GET, "/prefix/a/something"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("header"), equalTo("value0"))
        assertThat(response.bodyString(), equalTo("matched"))
    }

    @Test
    fun `group router shortcuts if parent prefix does not match`() {
        val app = routes("/prefix" bind routes(
            "/{.*}" to GET bind { Response(OK).body("matched") }
        ))

        assertThat(app(Request(GET, "/prefix/a/something")).status, equalTo(OK))
        assertThat(app(Request(GET, "/notprefix/a/something")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `route grouping prefix can contain a dynamic segment`() {
        val subRoutes = routes(
            "/a/{route}" to GET bind { Response(OK).body(it.path("name") + it.path("route")) }
        )

        val app = routes("/{name:\\d+}" bind subRoutes)

        assertThat(app(Request(GET, "/123/a/something")).status, equalTo(OK))
        assertThat(app(Request(GET, "/123/a/something")).bodyString(), equalTo("123something"))
        assertThat(app(Request(GET, "/asd/a/something")).status, equalTo(NOT_FOUND))
    }

    @Test
    fun `can add filter to router`() {
        val changePathFilter = Filter { next ->
            { next(it.uri(it.uri.path("/svc/mybob.xml"))) }
        }
        val handler = "/svc" bind changePathFilter.then(static())
        val req = Request(GET, Uri.of("/svc/notmybob.xml"))
        assertThat(handler(req).status, equalTo(OK))
    }

    @Test
    fun `can add filter to a RoutingHttpHandler`() {
        val changePathFilter = Filter { next ->
            { next(it.uri(it.uri.path("/svc/mybob.xml"))) }
        }
        val handler = changePathFilter.then("/svc" bind static())
        val req = Request(GET, Uri.of("/svc/notmybob.xml"))
        assertThat(handler(req).status, equalTo(OK))
    }

    @Test
    fun `can apply a filter to a RoutingHttpHandler`() {
        val routes = Filter { next -> { next(it.header("name", "value")) } }
            .then({ Response(OK).body(it.header("name")!!) })

        val routingHttpHandler = routes(
            "/a/thing" to GET bind routes
        )
        assertThat(routingHttpHandler(Request(GET, "/a/thing")).bodyString(), equalTo("value"))
    }

    @Test
    fun `can apply a filter to a Router`() {
        val routes = Filter { next -> { next(it.header("name", "value")) } }
            .then(routes(
                "/a/thing" to GET bind { Response(OK).body(it.header("name")!!) }
            ))

        assertThat(routes(Request(GET, "/a/thing")).bodyString(), equalTo("value"))
    }

}
