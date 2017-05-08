package org.http4k.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Request.Companion.post
import org.http4k.http.core.Response.Companion.ok
import org.http4k.http.core.body.form
import org.http4k.http.core.body.toBody

class HttpHandlerTest {
    @Test
    fun basic_handler() {
        val handler = { _: Request -> ok() }
        val response = handler(get("irrelevant"))
        assertThat(response, equalTo(ok()))
    }

    @Test
    fun query_parameters() {
        val handler = { request: Request -> ok().body("Hello, ${request.query("name")}") }
        val response = handler(get("/").query("name", "John Doe"))
        assertThat(response, equalTo(ok().body("Hello, John Doe")))
    }

    @Test
    fun form_handling() {
        val handler = { request: Request -> ok().body("Hello, ${request.form("name")}") }
        val form = listOf("name" to "John Doe")

        val response = handler(post("irrelevant").body(form.toBody()))

        assertThat(response.bodyString(), equalTo("Hello, John Doe"))
    }
}

