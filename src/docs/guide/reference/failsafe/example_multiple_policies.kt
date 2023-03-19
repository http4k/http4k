package guide.reference.failsafe

import dev.failsafe.CircuitBreaker
import dev.failsafe.Failsafe
import dev.failsafe.Fallback
import dev.failsafe.RetryPolicy
import dev.failsafe.Timeout
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.FailsafeFilter
import java.time.Duration

fun main() {
    // Configure multiple Failsafe policies
    val failsafeExecutor = Failsafe.with(
        Fallback.of(Response(OK).body("Fallback")),
        RetryPolicy.builder<Response>()
            .withMaxAttempts(2)
            .handleResultIf { !it.status.successful }
            .build(),
        CircuitBreaker.builder<Response>()
            .withFailureThreshold(1)
            .handleResultIf { it.status.serverError }
            .build(),
        Timeout.of(Duration.ofMillis(100))
    )

    // We then create a very unstable client using the filter
    var requestCount = 0
    val client = FailsafeFilter(failsafeExecutor).then {
        requestCount++

        when (requestCount % 4) {
            1, 3 -> Response(OK).body("All good - $requestCount")
            0 -> Response(INTERNAL_SERVER_ERROR)
            else -> Response(BAD_REQUEST)
        }
    }

    // Throw a bunch of request at the filter - some will fail and be retried until
    // the circuit breaker opens and the fallback value will be used after that.
    for (it in 1..5) {
        val response = client(Request(GET, "/"))
        println("${response.status} - ${response.bodyString()}")
    }
}
