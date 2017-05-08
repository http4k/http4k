package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.client.ApacheHttpClient
import org.http4k.http.core.Method.GET
import org.http4k.http.core.Request
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response.Companion.ok
import org.http4k.http.routing.by
import org.http4k.http.routing.routes
import org.junit.After
import org.junit.Before
import org.junit.Test

class JettyServerTest {
    var server: JettyServer? = null
    private val client = ApacheHttpClient()

    @Before
    fun before() {
        server = routes(
            GET to "/" by { _: Request -> ok().body("Hello World") },
            GET to "/request-headers" by { request: Request -> ok().body(request.headerValues("foo").joinToString(", ")) }
        ).startJettyServer(block = false)
    }

    @Test
    fun can_use_as_servlet() {
        val client = client
        val response = client(get("http://localhost:8000/"))

        assertThat(response.bodyString(), equalTo("Hello World"))
    }

    @Test
    fun can_handle_multiple_request_headers() {
        val client = client
        val response = client(get("http://localhost:8000/request-headers", listOf("foo" to "one", "foo" to "two", "foo" to "three")))

        assertThat(response.bodyString(), equalTo("one, two, three"))
    }

    @After
    fun after() {
        server?.stop()
    }

}

