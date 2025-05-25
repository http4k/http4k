package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.junit.jupiter.api.Test

class CorsAndRebindProtectionTest {

    private val allowedOrigin = "https://allowed-origin.com"
    private val disallowedOrigin = "https://disallowed-origin.com"

    private val corsPolicy = CorsPolicy(
        OriginPolicy.Only(allowedOrigin),
        listOf("Content-Type", "Authorization"),
        listOf(GET, POST, OPTIONS),
        credentials = true
    )

    private val testHttpHandler: HttpHandler = { Response(OK) }
    private val testSseHandler: SseHandler = { SseResponse(OK, emptyList(), false) {} }

    private val corsAndRebindFilter = ServerFilters.CorsAndRebindProtection(corsPolicy)

    private val protectedPolyHandler = corsAndRebindFilter.then(
        PolyHandler(
            http = testHttpHandler,
            sse = testSseHandler
        )
    )

    private val httpHandler = protectedPolyHandler.http!!

    @Test
    fun `allows HTTP requests with valid origin`() = runBlocking {
        val request = Request(GET, "/api")
            .header("Origin", allowedOrigin)

        val response = httpHandler(request)

        assertThat(response.status, equalTo(OK))
        assertThat(response, hasHeader("Access-Control-Allow-Origin", allowedOrigin))
    }

    @Test
    fun `allows HTTP requests with no origin header`() = runBlocking {
        val request = Request(GET, "/api")

        val response = httpHandler(request)

        assertThat(response.status, equalTo(OK))
        assertThat(response.headers.any { it.first.startsWith("Access-Control") }, equalTo(false))
    }

    @Test
    fun `handles preflight requests correctly`() = runBlocking {
        val request = Request(OPTIONS, "/api")
            .header("Origin", allowedOrigin)
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "Content-Type")

        val response = httpHandler(request)

        assertThat(response.status, equalTo(OK))
        assertThat(response, hasHeader("Access-Control-Allow-Origin", allowedOrigin))
        assertThat(response, hasHeader("Access-Control-Allow-Methods"))
        assertThat(response, hasHeader("Access-Control-Allow-Headers"))
    }

    @Test
    fun `blocks HTTP requests with disallowed origin`() = runBlocking {
        val request = Request(GET, "/api")
            .header("Origin", disallowedOrigin)

        val response = httpHandler(request)

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("Access-Control-Allow-Origin") == disallowedOrigin, equalTo(false))
    }

    private val sseHandler = protectedPolyHandler.sse!!

    @Test
    fun `allows SSE requests with valid origin`() = runBlocking {
        val request = Request(GET, "/sse")
            .header("Origin", allowedOrigin)

        val response = sseHandler(request)

        assertThat(response.status, equalTo(OK))
    }

    @Test
    fun `blocks SSE requests with disallowed origin`() = runBlocking {
        val request = Request(GET, "/sse")
            .header("Origin", disallowedOrigin)

        val response = sseHandler(request)

        assertThat(response.status, equalTo(FORBIDDEN))
    }

    @Test
    fun `blocks SSE requests with missing origin header`() = runBlocking {
        val request = Request(GET, "/sse")

        val response = sseHandler(request)

        assertThat(response.status, equalTo(FORBIDDEN))
    }

    @Test
    fun `can be composed with other poly filters`() = runBlocking {
        val loggingFilter = PolyFilter { next ->
            PolyHandler(
                http = next.http?.let { http -> { req: Request -> http(req) } },
                sse = next.sse?.let { sse -> { req: Request -> sse(req) } }
            )
        }

        val composedHandler = loggingFilter
            .then(corsAndRebindFilter)
            .then(
                PolyHandler(
                    http = testHttpHandler,
                    sse = testSseHandler
                )
            )

        val httpRequest = Request(GET, "/api")
            .header("Origin", allowedOrigin)
        val httpResponse = composedHandler.http!!(httpRequest)
        assertThat(httpResponse.status, equalTo(OK))

        val sseRequest = Request(GET, "/sse")
            .header("Origin", allowedOrigin)
        val sseResponse = composedHandler.sse!!(sseRequest)
        assertThat(sseResponse.status, equalTo(OK))
    }
}
