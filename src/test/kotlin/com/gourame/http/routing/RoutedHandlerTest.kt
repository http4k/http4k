package com.gourame.http.routing

import com.gourame.http.core.Entity
import com.gourame.http.core.Method.GET
import com.gourame.http.core.Request
import com.gourame.http.core.Request.Companion.get
import com.gourame.http.core.Request.Companion.post
import com.gourame.http.core.Response
import com.gourame.http.core.Status.Companion.METHOD_NOT_ALLOWED
import com.gourame.http.core.Status.Companion.NOT_FOUND
import com.gourame.http.core.Status.Companion.OK
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
}