package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.withAsyncApi
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class AsyncHttpHandlerTest: PortBasedTest {
    @Test
    fun `can convert a synchronous HttpHandler to mimic the AsyncHttpClient API`() = runBlocking {

        val handler = { _: Request -> Response(OK) }.withAsyncApi()

        val latch = CountDownLatch(1)
        handler(Request(GET, "/")) {
            assertThat(it.status, equalTo(OK))
            latch.countDown()
        }

        latch.await()
    }
}
