package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.by
import org.http4k.routing.routes
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

abstract class ServerContract(private val serverConfig: (Int) -> ServerConfig, private val client: HttpHandler) {
    private var server: Http4kServer? = null

    private val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        server = routes(
            Method.GET to "/" by { _: Request -> Response(Status.OK).body("Hello World") },
            Method.GET to "/request-headers" by { request: Request -> Response(Status.OK).body(request.headerValues("foo").joinToString(", ")) }
        ).asServer(serverConfig(port)).start()
    }

    @Test
    fun can_use_as_servlet() {
        val client = client
        val response = client(Request(Method.GET, "http://localhost:$port/"))

        assertThat(response.bodyString(), equalTo("Hello World"))
    }

    @Test
    fun can_handle_multiple_request_headers() {
        val client = client
        val response = client(Request(Method.GET, "http://localhost:$port/request-headers").header("foo", "one").header("foo", "two").header("foo", "three"))

        assertThat(response.bodyString(), equalTo("one, two, three"))
    }

    @After
    fun after() {
        server?.stop()
    }

}