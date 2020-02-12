package org.http4k.client

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.server.Jetty
import org.junit.jupiter.api.Test

class OkHttpTest : HttpClientContract({ Jetty(it) }, OkHttp(), OkHttp(timeout)) {

    @Test
    fun `can modify requests`() {
        assertThat(
            OkHttp(requestModifier = { it.header("header", "321") })(Request(POST, "http://localhost:$port/someUri")),
            hasStatus(OK) and hasHeader("header", "321")
        )
    }
}