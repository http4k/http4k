package org.http4k.filter

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import dev.failsafe.Bulkhead
import dev.failsafe.CircuitBreaker
import dev.failsafe.Failsafe
import dev.failsafe.FailsafeException
import dev.failsafe.Fallback
import dev.failsafe.RateLimiter
import dev.failsafe.Timeout
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class FailsafeFilterTest {

    @Test
    fun `response is returned when failsafe policies pass`() {
        val withFailsafe = FailsafeFilter(Failsafe.none()).then { Response(Status.OK).body("All OK") }

        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(Status.OK) and hasBody("All OK"))
    }

    @Test
    fun `default response is returned on circuit breaker error`() {
        val executor = Failsafe.with(
            CircuitBreaker.builder<Response>()
                .withFailureThreshold(1)
                .handleResultIf { !it.status.successful }
                .build()
        )
        val withFailsafe = FailsafeFilter(executor).then { Response(Status.INTERNAL_SERVER_ERROR) }

        // Second request should fail because the circuit breaker is open
        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(Status.INTERNAL_SERVER_ERROR))
        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(503, "Circuit is open"))
    }

    @Test
    fun `default response is returned on bulkhead error`() {
        val executor = Failsafe.with(Bulkhead.of<Response>(1))

        val latch = CountDownLatch(1)
        val withFailsafe = FailsafeFilter(executor).then {
            latch.countDown()
            Thread.sleep(1000)
            Response(Status.OK)
        }

        thread {
            withFailsafe(Request(Method.GET, ""))
        }
        latch.await()

        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(429, "Bulkhead limit exceeded"))
    }

    @Test
    fun `default response is returned on timeout error`() {
        val executor = Failsafe.with(Timeout.of<Response>(Duration.ofMillis(10)))

        val withFailsafe = FailsafeFilter(executor).then {
            Thread.sleep(50)
            Response(Status.OK)
        }

        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(Status.CLIENT_TIMEOUT))
    }

    @Test
    fun `default response is returned on rate limiter error`() {
        val executor = Failsafe.with(
            RateLimiter.smoothBuilder<Response>(1, Duration.ofMillis(100)).build()
        )

        val withFailsafe = FailsafeFilter(executor).then { Response(Status.OK) }

        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(Status.OK))
        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(429, "Rate limit exceeded"))
    }

    @Test
    fun `uses result of fallback policy on error`() {
        val executor = Failsafe.with(
            Fallback.of(Response(Status.OK).body("Fallback")),
            RateLimiter.smoothBuilder<Response>(1, Duration.ofMillis(100)).build()
        )

        val withFailsafe = FailsafeFilter(executor).then { Response(Status.OK).body("All OK") }

        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(Status.OK) and hasBody("All OK"))
        assertThat(withFailsafe(Request(Method.GET, "")), hasStatus(Status.OK) and hasBody("Fallback"))
    }

    @Test
    fun `default error handler propagates unhandled failsafe errors`() {
        val withFailsafe = FailsafeFilter(Failsafe.none()).then { throw IOException("Boom") }

        assertThat(
            { withFailsafe(Request(Method.GET, "")) },
            throws(has(FailsafeException::cause, present(isA(has(IOException::message, equalTo("Boom"))))))
        )
    }

    companion object {
        private fun hasStatus(code: Int, description: String): Matcher<Response> =
            has(Response::status,
                has(Status::code, equalTo(code)) and has(Status::description, equalTo(description))
            )
    }
}
