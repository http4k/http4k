package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.hamkrest.hasStatus
import org.http4k.server.Jetty
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit.MILLISECONDS

class OkHttpTest : HttpClientContract({ Jetty(it) }, OkHttp(), OkHttp(timeout)) {

    @Test
    fun `can modify timeout`() {
        assertThat(
            OkHttp(timeoutModifier = { timeout(100, MILLISECONDS) })(Request(GET, "http://localhost:$port/delay/150")),
            hasStatus(CLIENT_TIMEOUT)
        )
    }
}