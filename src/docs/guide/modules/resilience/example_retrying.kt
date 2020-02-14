package guide.modules.resilience

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import java.util.ArrayDeque

fun main() {

    // configure the retry filter here, with max attempts and backoff
    val retry = Retry.of("retrying", RetryConfig.custom<RetryConfig>()
        .maxAttempts(3)
        .intervalFunction { attempt: Int -> (attempt * 2).toLong() }
        .build())

    // queued up responses
    val responses = ArrayDeque<Response>()
    responses.add(Response(Status.INTERNAL_SERVER_ERROR))
    responses.add(Response(Status.OK))

    val retrying = ResilienceFilters.RetryFailures(retry,
        isError = { r: Response -> !r.status.successful }
    ).then {
        val response = responses.removeFirst()
        println("trying request, will return " + response.status)
        response
    }

    println(retrying(Request(GET, "/")))
}
