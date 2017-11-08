package org.http.filter

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN
import io.github.resilience4j.circuitbreaker.CircuitBreaker.of
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import org.http4k.hamkrest.hasStatus
import org.junit.Test
import java.time.Duration
import java.util.*

class ResilienceFiltersTest {

    @Test
    fun `circuit breaker filter`() {
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

        val circuited = ResilienceFilters.Circuit(circuitBreaker).then { responses.removeFirst() }

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
}