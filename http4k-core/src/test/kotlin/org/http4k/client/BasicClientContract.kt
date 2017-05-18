package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

abstract class BasicClientContract(private val serverConfig: (Int) -> ServerConfig, private val client: HttpHandler) {
    private var server: Http4kServer? = null

    private val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        server = { request: Request ->
            Response(Status.OK)
                .header("uri", request.uri.toString())
                .header("header", request.header("header"))
                .header("query", request.query("query"))
                .body(request.body)
        }.asServer(serverConfig(port)).start()
    }

    @After
    fun after() {
        server?.stop()
    }

    @Test
    fun `can make call`() {
        val response = client(Request(Method.POST, "http://localhost:$port/someUri")
            .query("query", "123")
            .header("header", "value").body("body"))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.header("uri"), equalTo("/someUri?query=123"))
        assertThat(response.header("query"), equalTo("123"))
        assertThat(response.header("header"), equalTo("value"))
        assertThat(response.bodyString(), equalTo("body"))
    }
}