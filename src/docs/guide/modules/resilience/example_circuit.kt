package guide.modules.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import java.time.Duration
import java.util.ArrayDeque

// Circuit state transition: CLOSED (ok) -> OPEN (dead) -> HALF_OPEN (test) -> CLOSED (ok)
fun main(args: Array<String>) {

    // these example responses are queued up to trigger the circuit state changes
    val responses = ArrayDeque<Response>()
    responses.add(Response(Status.INTERNAL_SERVER_ERROR))
    responses.add(Response(Status.OK))
    responses.add(Response(Status.OK))

    // configure the circuit breaker filter here
    val circuitBreaker = CircuitBreaker.of("circuit",
        CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(2)
            .ringBufferSizeInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .build()
    )

    val circuited = ResilienceFilters.CircuitBreak(circuitBreaker,
        isError = { r: Response -> !r.status.successful } // this defaults to >= 500
    ).then { responses.removeFirst() }

    println("Result: " + circuited(Request(Method.GET, "/")).status + " Circuit is: " + circuitBreaker.state)
    println("Result: " + circuited(Request(Method.GET, "/")).status + " Circuit is: " + circuitBreaker.state)
    Thread.sleep(1100) // wait for reset
    println("Result: " + circuited(Request(Method.GET, "/")).status + " Circuit is: " + circuitBreaker.state)
    println("Result: " + circuited(Request(Method.GET, "/")).status + " Circuit is: " + circuitBreaker.state)
}