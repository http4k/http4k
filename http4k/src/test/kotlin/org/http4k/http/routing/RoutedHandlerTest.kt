package org.http4k.http.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.core.Method.GET
import org.http4k.http.core.Request
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Request.Companion.post
import org.http4k.http.core.Response.Companion.notFound
import org.http4k.http.core.Response.Companion.ok
import org.http4k.http.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.http.core.Status.Companion.NOT_FOUND
import org.junit.Assert.fail
import org.junit.Test

class RoutedHandlerTest {

    @Test
    fun not_found() {
        val routes = routes()

        val response = routes(get("/a/something"))

        assertThat(response.status, equalTo(NOT_FOUND))
        assertThat(response.status.description, equalTo("Route not found"))
    }

    @Test
    fun method_not_allowed() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> ok().body("matched") }
        )

        val response = routes(post("/a/something"))

        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun matches_uri_template_and_method() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> ok().body("matched") }
        )

        val response = routes(get("/a/something"))

        assertThat(response.bodyString(), equalTo("matched"))
    }

    @Test
    fun matches_uses_first_match() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> ok().body("matched a") },
            GET to "/a/{route}" by { _: Request -> ok().body("matched b") }
        )

        val response = routes(get("/a/something"))

        assertThat(response.bodyString(), equalTo("matched a"))
    }

    @Test
    fun path_parameters_are_available_in_request() {
        val routes = routes(
            GET to "/{a}/{b}/{c}" by { req: Request -> ok().body("matched ${req.path("a")}, ${req.path("b")}, ${req.path("c")}") }
        )

        val response = routes(get("/x/y/z"))

        assertThat(response.bodyString(), equalTo("matched x, y, z"))
    }

    @Test
    fun matches_uri_with_query() {
        val routes = routes(GET to "/a/b" by { ok() })

        val response = routes(get("/a/b?foo=bar"))

        assertThat(response, equalTo(ok()))
    }

    @Test
    fun matches_request_with_extra_path_parts(){
        val routes = routes(GET to "/a" by { ok() })

        val response = routes(get("/a/b"))

        assertThat(response, equalTo(ok()))
    }

    @Test
    fun can_stop_matching_extra_parts(){
        val routes = routes(GET to "/a{$}" by { ok() })

        val response = routes(get("/a/b"))

        assertThat(response, equalTo(notFound()))
    }

    @Test
    fun breaks_if_trying_to_access_path_parameters_without_header_present() {
        try {
            get("/").path("abc")
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertThat(e.message, equalTo("x-uri-template header not present in the request"))
        }
    }
}