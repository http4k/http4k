package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test
import java.time.Duration

class Java8HttpClientTest : HttpClientContract(
    serverConfig = ::ApacheServer,
    client = Java8HttpClient(),
    timeoutClient = Java8HttpClient(readTimeout = Duration.ofMillis(100))
),
    HttpClientWithMemoryModeContract {

    @Test
    fun `allow configuring read timeout`() {
        val client = Java8HttpClient(readTimeout = Duration.ofMillis(10))
        val response = client(Request(Method.GET, "http://localhost:$port/delay").query("millis", "50"))
        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }

    @Test
    fun `allow configuring connect timeout`() {
        val client = Java8HttpClient(connectionTimeout = Duration.ofMillis(10))
        val response = client(Request(Method.GET, "http://120.0.0.0:81/does-not-exist")) //non-routable host/port
        assertThat(response.status, equalTo(CLIENT_TIMEOUT))
    }
}
