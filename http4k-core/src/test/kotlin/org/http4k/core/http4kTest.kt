package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.Test
import java.util.concurrent.CountDownLatch

class AsyncTest {
    @Test
    fun `can convert a synchronous HttpHandler to mimic the async API`() {

        val handler = { _: Request -> Response(OK) }.withAsyncApi()

        val latch = CountDownLatch(1)
        handler(Request(Method.GET, "/")) { response ->
            assertThat(response.status, equalTo(OK))
            latch.countDown()
        }

        latch.await()
    }
}
