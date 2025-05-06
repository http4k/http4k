package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.junit.jupiter.api.Test

class SseRebindProtectionTest {

    private val allowedOrigin = "https://allowed-origin.com"
    private val disallowedOrigin = "https://disallowed-origin.com"

    private val corsPolicy = CorsPolicy(
        OriginPolicy.Only(allowedOrigin),
        listOf("Content-Type", "Authorization"),
        listOf(GET, Method.POST, Method.OPTIONS),
        credentials = true
    )

    private val testSseHandler: SseHandler = { SseResponse(OK, emptyList(), false) {} }

    private val handler = ServerFilters.SseRebindProtection(corsPolicy).then(testSseHandler)

    @Test
    fun `allows requests with valid origin`() {
        val request = Request(GET, "/sse")
            .header("Origin", allowedOrigin)

        assertThat(handler(request).status, equalTo(OK))
    }

    @Test
    fun `blocks requests with disallowed origin`() {
        val request = Request(GET, "/sse").header("Origin", disallowedOrigin)

        assertThat(handler(request).status, equalTo(FORBIDDEN))
    }

    @Test
    fun `blocks requests with missing origin header`() {
        val request = Request(GET, "/sse")

        assertThat(handler(request).status, equalTo(FORBIDDEN))
    }
}
