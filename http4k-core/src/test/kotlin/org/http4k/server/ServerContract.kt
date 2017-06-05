package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.startsWith
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
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
            "/" to GET by { _: Request ->
                Response(ACCEPTED)
                    .header("content-type", "text/plain")
                    .body("Hello World")
            },
            "/echo" to POST by { req: Request -> Response(OK).body(req.bodyString()) },
            "/request-headers" to GET by { request: Request -> Response(OK).body(request.headerValues("foo").joinToString(", ")) },
            "/uri" to GET by { req: Request -> Response(OK).body(req.uri.toString()) },
            "/boom" to GET by { _: Request -> throw IllegalArgumentException("BOOM!") }
        ).startServer(serverConfig(port), false)
    }

    @Test
    fun `can call an endpoint`() {
        val response = client(Request(GET, "http://localhost:$port/"))

        assertThat(response.status, equalTo(ACCEPTED))
        assertThat(response.header("content-type"), present(startsWith("text/plain")))
        assertThat(response.bodyString(), equalTo("Hello World"))
    }

    @Test
    fun `gets the body from the request`() {
        val response = client(Request(POST, "http://localhost:$port/echo").body("hello mum"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello mum"))
    }

    @Test
    fun `gets the uri from the request`() {
        val response = client(Request(GET, "http://localhost:$port/uri?bob=bill"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("/uri?bob=bill"))
    }

    @Test
    fun `endpoint that blows up results in 500`() {
        val response = client(Request(GET, "http://localhost:$port/boom"))

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `can handle multiple request headers`() {
        val response = client(Request(GET, "http://localhost:$port/request-headers").header("foo", "one").header("foo", "two").header("foo", "three"))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("one, two, three"))
    }

    @After
    fun after() {
        server?.stop()
    }

}