package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

class RoutingTest {

    @Test
    fun `not found`() {
        val routes = routes()

        val response = routes(Request(GET, "/a/something"))

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.status.description, equalTo("Route not found"))
    }

    @Test
    @Ignore
    fun `method not allowed`() {
        val routes = routes(
            GET to "/a/{route}" by { Response(OK).body("matched") }
        )

        val response = routes(Request(POST, "/a/something"))

        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun `matches uri template and method`() {
        val routes = routes(
            GET to "/a/{route}" by { Response(OK).body("matched") }
        )

        val response = routes(Request(GET, "/a/something"))

        assertThat(response.bodyString(), equalTo("matched"))
    }

    @Test
    fun `matches uses first match`() {
        val routes = routes(
            GET to "/a/{route}" by { Response(OK).body("matched a") },
            GET to "/a/{route}" by { Response(OK).body("matched b") }
        )

        val response = routes(Request(GET, "/a/something"))

        assertThat(response.bodyString(), equalTo("matched a"))
    }

    @Test
    fun `path parameters are available in request`() {
        val routes = routes(
            GET to "/{a}/{b}/{c}" by { req: Request -> Response(OK).body("matched ${req.path("a")}, ${req.path("b")}, ${req.path("c")}") }
        )

        val response = routes(Request(GET, "/x/y/z"))

        assertThat(response.bodyString(), equalTo("matched x, y, z"))
    }

    @Test
    fun `matches uri with query`() {
        val routes = routes(GET to "/a/b" by { Response(OK) })

        val response = routes(Request(GET, "/a/b?foo=bar"))

        assertThat(response, equalTo(Response(OK)))
    }

    @Test
    fun `matches request with extra path parts`() {
        val routes = routes(GET to "/a" by { Response(OK) })

        val response = routes(Request(GET, "/a/b"))

        assertThat(response, equalTo(Response(OK)))
    }

    @Test
    fun `can stop matching extra parts`() {
        val routes = routes(GET to "/a{$}" by { Response(OK) })

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
            GET to "/a/{route}" by { Response(OK).body("matched") }
        )

        val app = routes("/prefix" by subRoutes)

        val response = app(Request(GET, "/prefix/a/something"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("matched"))
    }

}