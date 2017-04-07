package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.body.bodyForm
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.body.form
import org.reekwest.http.core.body.string

class HttpHandlerTest {
    @Test
    fun basic_handler() {
        val handler = { _: Request -> ok() }
        val response = handler(get("irrelevant"))
        assertThat(response, equalTo(ok()))
    }

    @Test
    fun query_parameters() {
        val handler = { request: Request -> ok().bodyString("Hello, ${request.query("name")}") }
        val response = handler(get("/").query("name", "John Doe"))
        assertThat(response, equalTo(ok().bodyString("Hello, John Doe")))
    }

    @Test
    fun form_handling() {
        val handler = { request: Request -> ok().bodyString("Hello, ${request.form("name")}") }
        val form = listOf("name" to "John Doe")

        val response = handler(post("irrelevant").bodyForm(form))

        assertThat(response.body.string(), equalTo("Hello, John Doe"))
    }
}

