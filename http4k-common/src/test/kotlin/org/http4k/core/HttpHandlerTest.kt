package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.http4k.core.body.toBody
import org.junit.jupiter.api.Test

class HttpHandlerTest {
    @Test
    fun basic_handler() {
        val handler = { _: Request -> Response(OK) }
        val response = handler(Request(GET, "irrelevant"))
        assertThat(response, equalTo(Response(OK)))
    }

    @Test
    fun query_parameters() {
        val handler = { request: Request -> Response(OK).body("Hello, ${request.query("name")}") }
        val response = handler(Request(GET, "/").query("name", "John Doe"))
        assertThat(response, equalTo(Response(OK).body("Hello, John Doe")))
    }

    @Test
    fun form_handling() {
        val handler = { request: Request -> Response(OK).body("Hello, ${request.form("name")}") }
        val form = listOf("name" to "John Doe")

        val response = handler(Request(POST, "irrelevant").body(form.toBody()))

        assertThat(response.bodyString(), equalTo("Hello, John Doe"))
    }
}
