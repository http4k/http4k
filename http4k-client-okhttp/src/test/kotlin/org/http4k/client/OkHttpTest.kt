package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.server.SunHttp
import org.junit.Test
import java.util.concurrent.CountDownLatch

class OkHttpTest : Http4kClientContract<SyncAsyncHttpHandler>({ SunHttp(it) }, OkHttp(), OkHttp(timeout)) {

    @Test
    fun `can make async call`() {
        val latch = CountDownLatch(1)
        client(Request(Method.POST, "http://localhost:$port/someUri")
            .query("query", "123")
            .header("header", "value").body("body")) { response ->
            assertThat(response.status, equalTo(Status.OK))
            assertThat(response.header("uri"), equalTo("/someUri?query=123"))
            assertThat(response.header("query"), equalTo("123"))
            assertThat(response.header("header"), equalTo("value"))
            assertThat(response.bodyString(), equalTo("body"))
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun `async socket timeouts are converted into 504`() {
        val latch = CountDownLatch(1)
        timeoutClient(Request(Method.GET, "http://localhost:$port/delay/150")) { response ->
            assertThat(response.status, equalTo(Status.CLIENT_TIMEOUT))
            latch.countDown()
        }

        latch.await()
    }
}
