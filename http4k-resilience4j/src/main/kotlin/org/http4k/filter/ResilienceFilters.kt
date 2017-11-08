package org.http4k.filter

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.Retry.ofDefaults
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.TOO_MANY_REQUESTS

object ResilienceFilters {

    /**
     * Provide simple Circuit Breaker. Returns ServiceUnavailable when the circuit is open.
     * By default, uses a % failure rate of 50% detection and an Circuit Open period of 1minute
     */
    object CircuitBreak {
        private object CircuitError : Exception()

        operator fun invoke(circuitBreaker: CircuitBreaker = CircuitBreaker.ofDefaults("Circuit"),
                            isError: (Response) -> Boolean = { it.status.serverError },
                            onError: () -> Response = { Response(SERVICE_UNAVAILABLE.description("Circuit is open")) }) = Filter { next ->
            {
                try {
                    circuitBreaker.executeCallable {
                        next(it).apply {
                            if (isError(this)) circuitBreaker.onError(0, CircuitError)
                        }
                    }
                } catch (e: CircuitBreakerOpenException) {
                    onError()
                }
            }
        }
    }

    /**
     * Provide simple Retrying functionality. Returns the last response when retries expire.
     * By default, retries 3 times with a delay of 500ms between attempts, backing off at a 1.5x multiplier.
     */
    object RetryFailures {

        private class RetryError(val response: Response) : RuntimeException()

        operator fun invoke(retry: Retry = ofDefaults("Retrying"),
                            isError: (Response) -> Boolean = { it.status.serverError }) = Filter { next ->
            {
                try {
                    retry.executeCallable {
                        next(it).apply {
                            if (isError(this)) throw RetryError(this)
                        }
                    }
                } catch (e: RetryError) {
                    e.response
                }
            }
        }
    }

    object RateLimit {
        operator fun invoke(rateLimit: RateLimiter = RateLimiter.ofDefaults("RateLimit"),
                            onError: () -> Response = { Response(TOO_MANY_REQUESTS.description("Rate limit exceeded")) }) = Filter { next ->
            {
                try {
                    rateLimit.executeCallable { next(it) }
                } catch (e: RequestNotPermitted) {
                    onError()
                }
            }
        }
    }
}