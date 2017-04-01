package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Status.Companion.OK

class HttpHandlerTest {
    @Test
    fun basic_handler() {
        val handler = { _: Request -> Response(OK) }
        val response = handler(get("irrelevant"))
        assertThat(response, equalTo(Response(OK)))
    }

    @Test
    fun query_parameters() {
        val handler = { request: Request -> Response(OK, entity = "Hello, ${request.query("name")}".toEntity()) }
        val response = handler(get("/").query("name", "John Doe"))
        assertThat(response, equalTo(Response(OK, entity = "Hello, John Doe".toEntity())))
    }

    @Test
    fun form_handling() {
        val handler = { request: Request -> Response(OK, entity = Entity("Hello, ${request.form("name")}")) }
        val form = listOf("name" to "John Doe")

        val response = handler(post("irrelevant", listOf("content-type" to APPLICATION_FORM_URLENCODED), form.toEntity()))

        assertThat(response.extract(StringEntity), equalTo("Hello, John Doe"))
    }
}

