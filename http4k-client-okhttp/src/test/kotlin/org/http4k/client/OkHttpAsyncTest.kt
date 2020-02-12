package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.hamkrest.hasStatus
import org.http4k.server.SunHttp
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS

class OkHttpAsyncTest : AsyncHttpClientContract({ SunHttp(it) }, OkHttp(), OkHttp(timeout)) {

    @Test
    fun `can modify timeout`() {
        val response = CompletableFuture<Response>()
        OkHttp(timeoutModifier = { timeout(100, MILLISECONDS) })(Request(Method.GET, "http://localhost:$port/delay/150")) {
            response.complete(it)
        }

        assertThat(response.get(), hasStatus(CLIENT_TIMEOUT))
    }
}