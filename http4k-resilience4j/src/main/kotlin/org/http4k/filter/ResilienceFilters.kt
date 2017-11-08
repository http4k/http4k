package org.http4k.filter

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException
import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE

object ResilienceFilters {

    /**
     * Provide simple Circuit Breaker. Returns ServiceUnavailable when the circuit is open.
     * By default, uses a % failure rate of 50% detection and an Circuit Open period of 1minute
     */
    object Circuit {
        private object CircuitError : Exception()

        operator fun invoke(circuitBreaker: CircuitBreaker = CircuitBreaker.ofDefaults("CircuitFilter"),
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
}