package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.startServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

abstract class Http4kClientContract(private val serverConfig: (Int) -> ServerConfig, private val client: HttpHandler) {
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
        }.startServer(serverConfig(port), false)
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

    @Test
    fun `performs simple request`() {
        val response = client(Request(Method.GET, "http://httpbin.org/get").query("name", "John Doe"))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.bodyString(), containsSubstring("John Doe"))
    }

    @Test
    fun `does not follow redirects`() {
        val response = client(Request(Method.GET, "http://httpbin.org/redirect-to").query("url", "/destination"))

        assertThat(response.status, equalTo(Status.FOUND))
        assertThat(response.header("location"), equalTo("/destination"))
    }

    @Test
    fun `does not store cookies`() {
        client(Request(Method.GET, "http://httpbin.org/cookies/set").query("foo", "bar"))

        val response = client(Request(Method.GET, "http://httpbin.org/cookies"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), !containsSubstring("foo"))
    }

    @Test
    fun `filters enable cookies and redirects`() {
        val enhancedClient = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(client)

        val response = enhancedClient(Request(Method.GET, "http://httpbin.org/cookies/set").query("foo", "bar"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), containsSubstring("foo"))
    }
}