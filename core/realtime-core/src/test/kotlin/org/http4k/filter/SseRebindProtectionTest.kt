package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.junit.jupiter.api.Test
import java.awt.SystemColor.control

class SseRebindProtectionTest {

    private val allowedOrigin = "https://allowed-origin.com"
    private val disallowedOrigin = "https://disallowed-origin.com"

    private val corsPolicy = CorsPolicy(
        OriginPolicy.Only(allowedOrigin),
        listOf("Content-Type", "Authorization"),
        listOf(GET, POST, OPTIONS),
        true,
        listOf("foo", "bar"),
    )

    private val testSseHandler: SseHandler = { SseResponse(OK, emptyList(), false) {} }

    private val handler = ServerFilters.SseRebindProtection(corsPolicy).then(testSseHandler)

    @Test
    fun `allows requests with valid origin`() = runBlocking {
        val request = Request(GET, "/sse").header("Origin", allowedOrigin)

        val response = handler(request)
        assertThat(response.status, equalTo(OK))
        assertThat(
            response.headers, equalTo(
                listOf(
                    "access-control-allow-origin" to "https://allowed-origin.com",
                    "access-control-allow-headers" to "Content-Type, Authorization",
                    "access-control-allow-methods" to "GET, POST, OPTIONS",
                    "access-control-allow-credentials" to "true",
                    "access-control-expose-headers" to "foo, bar",
                    "Vary" to "Origin"
                )
            )
        )
    }

    @Test
    fun `blocks requests with disallowed origin`() = runBlocking {
        val request = Request(GET, "/sse").header("Origin", disallowedOrigin)

        assertThat(handler(request).status, equalTo(FORBIDDEN))
    }

    @Test
    fun `blocks requests with missing origin header`() = runBlocking {
        val request = Request(GET, "/sse")

        assertThat(handler(request).status, equalTo(FORBIDDEN))
    }

}
