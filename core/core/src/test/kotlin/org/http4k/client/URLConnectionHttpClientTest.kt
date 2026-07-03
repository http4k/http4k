package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test
import java.time.Duration

class URLConnectionHttpClientTest : HttpClientContract(
    serverConfig = ::ApacheServer,
    client = URLConnectionHttpClient(),
    timeoutClient = URLConnectionHttpClient(readTimeout = Duration.ofMillis(100))
),
    HttpClientWithMemoryModeContract {

    @Test
    fun `allow configuring read timeout`() {
        val client = URLConnectionHttpClient(readTimeout = Duration.ofMillis(10))
        val response = client(Request(Method.GET, "http://localhost:$port/delay").query("millis", "50"))
        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }

    @Test
    fun `allow configuring connect timeout`() {
        val client = URLConnectionHttpClient(connectionTimeout = Duration.ofMillis(10))
        val response = client(Request(Method.GET, "http://120.0.0.0:81/does-not-exist")) // non-routable host/port
        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }
}
