package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.server.Jetty
import org.junit.jupiter.api.Test
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit.MILLISECONDS

class OkHttpTest : HttpClientContract({ Jetty(it) }, OkHttp(), OkHttp(timeout)) {

    @Test
    fun `can modify timeout`() {
        assertThat(
            { OkHttp(timeoutModifier = { timeout(100, MILLISECONDS) })(Request(GET, "http://localhost:$port/delay/150")) },
            throws<InterruptedIOException>()
        )
    }
}