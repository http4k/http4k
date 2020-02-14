package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.OK
import org.http4k.server.ServerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

abstract class AsyncHttpClientContract(serverConfig: (Int) -> ServerConfig,
                                       val client: AsyncHttpClient,
                                       private val timeoutClient: AsyncHttpClient) : AbstractHttpClientContract(serverConfig) {

    @AfterEach
    fun close() {
        client.close()
    }

    @Test
    fun `can make call`() {
        val latch = CountDownLatch(1)
        client(Request(POST, "http://localhost:$port/someUri")
            .query("query", "123")
            .header("header", "value").body("body")) { response ->
            assertThat(response.status, equalTo(OK))
            assertThat(response.header("uri"), equalTo("/someUri?query=123"))
            assertThat(response.header("query"), equalTo("123"))
            assertThat(response.header("header"), equalTo("value"))
            assertThat(response.bodyString(), equalTo("body"))
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun `socket timeouts are converted into 504`() {
        val latch = CountDownLatch(1)
        timeoutClient(Request(GET, "http://localhost:$port/delay/1500")) { response ->
            assertThat(response.status, equalTo(CLIENT_TIMEOUT))
            latch.countDown()
        }

        latch.await()
    }
}
