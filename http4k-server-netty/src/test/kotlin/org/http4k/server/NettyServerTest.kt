package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response.Companion.ok
import org.http4k.routing.by
import org.http4k.routing.routes
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class NettyServerTest {
    var server: NettyServer? = null
    private val client = ApacheClient()

    private val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        server = routes(
            GET to "/" by { _: Request -> ok().body("Hello World") },
            GET to "/request-headers" by { request: Request -> ok().body(request.headerValues("foo").joinToString(", ")) }
        ).asNettyServer(port).start()
    }

    @Test
    fun can_use_as_servlet() {
        val client = client
        val response = client(get("http://localhost:$port/"))

        assertThat(response.bodyString(), equalTo("Hello World"))
    }

    @Test
    fun can_handle_multiple_request_headers() {
        val client = client
        val response = client(get("http://localhost:$port/request-headers", listOf("foo" to "one", "foo" to "two", "foo" to "three")))

        assertThat(response.bodyString(), equalTo("one, two, three"))
    }

    @After
    fun after() {
        server?.stop()
    }

}

