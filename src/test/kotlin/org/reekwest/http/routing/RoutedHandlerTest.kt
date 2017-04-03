package org.reekwest.http.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Assert.fail
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status.Companion.METHOD_NOT_ALLOWED
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.entity.StringEntity
import org.reekwest.http.core.entity.entity
import org.reekwest.http.core.entity.extract
import org.reekwest.http.core.get
import org.reekwest.http.core.ok
import org.reekwest.http.core.post

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
            GET to "/a/{route}" by { _: Request -> ok().entity("matched") }
        )

        val response = routes(post("/a/something"))

        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun matches_uri_template_and_method() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> ok().entity("matched") }
        )

        val response = routes(get("/a/something"))

        assertThat(response.extract(StringEntity), equalTo("matched"))
    }

    @Test
    fun matches_uses_first_match() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> ok().entity("matched a") },
            GET to "/a/{route}" by { _: Request -> ok().entity("matched b") }
        )

        val response = routes(get("/a/something"))

        assertThat(response.extract(StringEntity), equalTo("matched a"))
    }

    @Test
    fun path_parameters_are_available_in_request() {
        val routes = routes(
            GET to "/{a}/{b}/{c}" by { req: Request -> ok().entity("matched ${req.path("a")}, ${req.path("b")}, ${req.path("c")}") }
        )

        val response = routes(get("/x/y/z"))
        assertThat(response.extract(StringEntity), equalTo("matched x, y, z"))
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