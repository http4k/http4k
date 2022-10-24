package org.http.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
import io.github.resilience4j.circuitbreaker.CircuitBreaker.of
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.TOO_MANY_REQUESTS
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import org.http4k.filter.ResilienceFilters.Bulkheading
import org.http4k.filter.ResilienceFilters.CircuitBreak
import org.http4k.filter.ResilienceFilters.RateLimit
import org.http4k.filter.ResilienceFilters.RetryFailures
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class ResilienceFiltersTest {

    @Test
    fun `circuit break filter`() {
        val minimumOpenStateApparently = Duration.ofSeconds(1)
        val config = CircuitBreakerConfig.custom()
            .slidingWindow(2, 2, COUNT_BASED)
            .permittedNumberOfCallsInHalfOpenState(2)
            .waitDurationInOpenState(minimumOpenStateApparently)
            .build()

        val responses = ArrayDeque<Response>().apply {
            add(Response(INTERNAL_SERVER_ERROR))
            add(Response(INTERNAL_SERVER_ERROR))
            add(Response(OK))
            add(Response(OK))
            add(Response(OK))
        }

        val circuitBreaker = of("hello", config)

        val circuited = CircuitBreak(circuitBreaker).then { responses.removeFirst() }

        assertThat(circuitBreaker.state, equalTo(CLOSED))
        assertThat(circuited(Request(GET, "/")), hasStatus(INTERNAL_SERVER_ERROR))
        assertThat(circuited(Request(GET, "/")), hasStatus(INTERNAL_SERVER_ERROR))
        assertThat(circuitBreaker.state, equalTo(OPEN))
        assertThat(circuited(Request(GET, "/")), hasStatus(SERVICE_UNAVAILABLE))
        Thread.sleep(1100)
        assertThat(circuited(Request(GET, "/")), hasStatus(OK))
        assertThat(circuitBreaker.state, equalTo(HALF_OPEN))
        assertThat(circuited(Request(GET, "/")), hasStatus(OK))
        assertThat(circuitBreaker.state, equalTo(CLOSED))
    }

    @Test
    fun `retrying stops when successful result returned`() {

        val config = RetryConfig.custom<RetryConfig>().intervalFunction { 0 }.build()
        val retry = Retry.of("retrying", config)

        val responses = ArrayDeque<Response>().apply {
            add(Response(INTERNAL_SERVER_ERROR))
            add(Response(OK))
        }

        val retrying = RetryFailures(retry).then {
            responses.removeFirst()
        }

        assertThat(retrying(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `retrying eventually runs out and returns the last result`() {

        val config = RetryConfig.custom<RetryConfig>().intervalFunction { 0 }.build()
        val retry = Retry.of("retrying", config)

        val responses = ArrayDeque<Response>().apply {
            add(Response(INTERNAL_SERVER_ERROR))
            add(Response(BAD_GATEWAY))
            add(Response(SERVICE_UNAVAILABLE))
        }
        val retrying = RetryFailures(retry).then {
            responses.removeFirst()
        }

        assertThat(retrying(Request(GET, "/")).status, equalTo(SERVICE_UNAVAILABLE))
    }

    @Test
    fun `rate limit filter`() {
        val config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofMillis(10)).build()

        val rateLimits = RateLimit(RateLimiter.of("ratelimiter", config)).then { Response(OK) }

        assertThat(rateLimits(Request(GET, "/")).status, equalTo(OK))
        assertThat(rateLimits(Request(GET, "/")).status, equalTo(TOO_MANY_REQUESTS))
    }

    @Test
    fun `bulkhead filter`() {
        val config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ZERO)
            .build()

        val latch = CountDownLatch(1)
        val bulkheading = Bulkheading(Bulkhead.of("bulkhead", config)).then {
            latch.countDown()
            Thread.sleep(1000)
            Response(OK)
        }

        thread {
            bulkheading(Request(GET, "/first"))
        }

        latch.await()
        assertThat(bulkheading(Request(GET, "/second")).status, equalTo(TOO_MANY_REQUESTS))
    }
}
