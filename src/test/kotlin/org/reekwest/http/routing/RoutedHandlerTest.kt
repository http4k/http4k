package org.reekwest.http.routing

import org.reekwest.http.core.Entity
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Request.Companion.post
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.METHOD_NOT_ALLOWED
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
            GET to "/a/{route}" by { _: Request -> Response(OK, entity = Entity("matched")) }
        )

        val response = routes(post("/a/something"))

        assertThat(response.status, equalTo(METHOD_NOT_ALLOWED))
    }

    @Test
    fun matches_uri_template_and_method() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> Response(OK, entity = Entity("matched")) }
        )

        val response = routes(get("/a/something"))

        assertThat(response.entity.toString(), equalTo("matched"))
    }

    @Test
    fun matches_uses_first_match() {
        val routes = routes(
            GET to "/a/{route}" by { _: Request -> Response(OK, entity = Entity("matched a")) },
            GET to "/a/{route}" by { _: Request -> Response(OK, entity = Entity("matched b")) }
        )

        val response = routes(get("/a/something"))

        assertThat(response.entity.toString(), equalTo("matched a"))
    }

    @Test
    fun path_parameters_are_available_in_request() {
        val routes = routes(
            GET to "/{a}/{b}/{c}" by { req: Request -> Response(OK, entity = Entity("matched ${req.pathParameter("a")}, ${req.pathParameter("b")}, ${req.pathParameter("c")}")) }
        )

        val response = routes(get("/x/y/z"))
        assertThat(response.entity.toString(), equalTo("matched x, y, z"))
    }
}