package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.entity.StringEntity
import org.reekwest.http.core.entity.entity
import org.reekwest.http.core.entity.extract
import org.reekwest.http.core.entity.form

class HttpHandlerTest {
    @Test
    fun basic_handler() {
        val handler = { _: Request -> ok() }
        val response = handler(get("irrelevant"))
        assertThat(response, equalTo(ok()))
    }

    @Test
    fun query_parameters() {
        val handler = { request: Request -> ok().entity("Hello, ${request.query("name")}") }
        val response = handler(get("/").query("name", "John Doe"))
        assertThat(response, equalTo(ok().entity("Hello, John Doe")))
    }

    @Test
    fun form_handling() {
        val handler = { request: Request -> ok().entity("Hello, ${request.form("name")}") }
        val form = listOf("name" to "John Doe")

        val response = handler(post("irrelevant").entity(form))

        assertThat(response.extract(StringEntity), equalTo("Hello, John Doe"))
    }
}

