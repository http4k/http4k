package org.http.filter

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
import io.github.resilience4j.circuitbreaker.CircuitBreaker.of
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
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
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class ResilienceFiltersTest {

    @Test
    fun `circuit break filter`() {
        val minimumOpenStateApparently = Duration.ofSeconds(1)
        val config = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(2)
            .ringBufferSizeInHalfOpenState(2)
            .waitDurationInOpenState(minimumOpenStateApparently)
            .build()

        val responses = ArrayDeque<Response>()
        responses.add(Response(INTERNAL_SERVER_ERROR))
        responses.add(Response(OK))
        responses.add(Response(OK))

        val circuitBreaker = of("hello", config)

        val circuited = ResilienceFilters.CircuitBreak(circuitBreaker).then { responses.removeFirst() }

        circuitBreaker.state shouldMatch equalTo(CLOSED)
        circuited(Request(GET, "/")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR)
        circuitBreaker.state shouldMatch equalTo(OPEN)
        circuited(Request(GET, "/")) shouldMatch hasStatus(SERVICE_UNAVAILABLE)
        Thread.sleep(1100)
        circuited(Request(GET, "/")) shouldMatch hasStatus(OK)
        circuitBreaker.state shouldMatch equalTo(HALF_OPEN)
        circuited(Request(GET, "/")) shouldMatch hasStatus(OK)
        circuitBreaker.state shouldMatch equalTo(CLOSED)
    }

    @Test
    fun `retrying stops when successful result returned`() {

        val config = RetryConfig.custom().intervalFunction { 0 }.build()
        val retry = Retry.of("retrying", config)

        val responses = ArrayDeque<Response>()
        responses.add(Response(INTERNAL_SERVER_ERROR))
        responses.add(Response(OK))

        val retrying = ResilienceFilters.RetryFailures(retry).then {
            responses.removeFirst()
        }

        retrying(Request(GET, "/")).status shouldMatch equalTo(OK)
    }

    @Test
    fun `retrying eventually runs out and returns the last result`() {

        val config = RetryConfig.custom().intervalFunction { 0 }.build()
        val retry = Retry.of("retrying", config)

        val responses = ArrayDeque<Response>()
        responses.add(Response(INTERNAL_SERVER_ERROR))
        responses.add(Response(BAD_GATEWAY))
        responses.add(Response(SERVICE_UNAVAILABLE))
        val retrying = ResilienceFilters.RetryFailures(retry).then {
            responses.removeFirst()
        }

        retrying(Request(GET, "/")).status shouldMatch equalTo(SERVICE_UNAVAILABLE)
    }

    @Test
    fun `rate limit filter`() {
        val config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofMillis(10)).build()

        val rateLimits = ResilienceFilters.RateLimit(RateLimiter.of("ratelimiter", config)).then {
            Thread.sleep(20)
            Response(OK)
        }

        rateLimits(Request(GET, "/")).status shouldMatch equalTo(OK)
        rateLimits(Request(GET, "/")).status shouldMatch equalTo(TOO_MANY_REQUESTS)
    }

    @Test
    fun `bulkhead filter`() {
        val config = BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitTime(0)
            .build()

        val bulkheading = ResilienceFilters.Bulkheading(Bulkhead.of("bulkhead", config)).then {
            Thread.sleep(10)
            Response(OK)
        }

        val latch = CountDownLatch(1)
        thread {
            latch.countDown()
            bulkheading(Request(GET, "/"))
        }

        latch.await()
        bulkheading(Request(GET, "/")).status shouldMatch equalTo(TOO_MANY_REQUESTS)
    }

}