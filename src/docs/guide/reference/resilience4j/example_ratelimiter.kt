package guide.reference.resilience4j

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import java.time.Duration

fun main() {

    // configure the rate limiter filter here
    val config = RateLimiterConfig.custom()
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .limitForPeriod(1)
        .timeoutDuration(Duration.ofMillis(10)).build()

    // set up the responses to sleep for a bit
    val rateLimits = ResilienceFilters.RateLimit(RateLimiter.of("ratelimiter", config))
        .then { Response(OK) }

    println(rateLimits(Request(GET, "/")).status)
    println(rateLimits(Request(GET, "/")).status)
}
