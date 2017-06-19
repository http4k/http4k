package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
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

        val routes =
            Method.values().map {
                "/" + it.name to it bind { _: Request ->
                    Response(OK).body(it.name)
                }
            }.plus(listOf(
                "/" to GET bind { _: Request ->
                    Response(ACCEPTED)
                        .header("content-type", "text/plain")
                        .body("Hello World")
                },
                "/echo" to POST bind { req: Request -> Response(OK).body(req.bodyString()) },
                "/request-headers" to GET bind { request: Request -> Response(OK).body(request.headerValues("foo").joinToString(", ")) },
                "/uri" to GET bind { req: Request -> Response(OK).body(req.uri.toString()) },
                "/boom" to GET bind { _: Request -> throw IllegalArgumentException("BOOM!") }
            ))

        server = routes(*routes.toTypedArray()).asServer(serverConfig(port)).start()
    }

    @Test
    fun `can call an endpoint with all supported Methods`() {
        for (method in Method.values()) {

            val response = client(Request(method, "http://localhost:$port/" + method.name))

            assertThat(response.status, equalTo(OK))
            assertThat(response.bodyString(), equalTo(method.name))
        }
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