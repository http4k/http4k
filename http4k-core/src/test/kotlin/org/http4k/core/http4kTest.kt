package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.withAsyncApi
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class AsyncHttpClientTest {
    @Test
    fun `can convert a synchronous HttpHandler to mimic the AsyncHttpClient API`() {

        val handler = { _: Request -> Response(OK) }.withAsyncApi()

        val latch = CountDownLatch(1)
        handler(Request(GET, "/")) {
            assertThat(it.status, equalTo(OK))
            latch.countDown()
        }

        latch.await()
    }
}
