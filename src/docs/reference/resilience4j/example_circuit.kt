package reference.resilience4j

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import java.time.Duration
import java.util.ArrayDeque

// Circuit state transition: CLOSED (ok) -> OPEN (dead) -> HALF_OPEN (test) -> CLOSED (ok)
fun main() {

    // these example responses are queued up to trigger the circuit state changes
    val responses = ArrayDeque<Response>()
    responses.add(Response(INTERNAL_SERVER_ERROR))
    responses.add(Response(OK))
    responses.add(Response(OK))

    // configure the circuit breaker filter here
    val circuitBreaker = CircuitBreaker.of("circuit",
        CircuitBreakerConfig.custom()
            .slidingWindow(2, 2, COUNT_BASED)
            .permittedNumberOfCallsInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .build()
    )

    val circuited = ResilienceFilters.CircuitBreak(circuitBreaker,
        isError = { r: Response -> !r.status.successful } // this defaults to >= 500
    ).then { responses.removeFirst() }

    println("Result: " + circuited(Request(GET, "/")).status + " Circuit is: " + circuitBreaker.state)
    println("Result: " + circuited(Request(GET, "/")).status + " Circuit is: " + circuitBreaker.state)
    Thread.sleep(1100) // wait for reset
    println("Result: " + circuited(Request(GET, "/")).status + " Circuit is: " + circuitBreaker.state)
    println("Result: " + circuited(Request(GET, "/")).status + " Circuit is: " + circuitBreaker.state)
}
