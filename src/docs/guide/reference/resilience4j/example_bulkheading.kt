package guide.reference.resilience4j

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ResilienceFilters
import java.time.Duration
import kotlin.concurrent.thread

fun main() {

    // configure the Bulkhead filter here
    val config = BulkheadConfig.custom()
        .maxConcurrentCalls(5)
        .maxWaitDuration(Duration.ofMillis(1000))
        .build()

    val bulkheading = ResilienceFilters.Bulkheading(Bulkhead.of("bulkhead", config)).then {
        Thread.sleep(100)
        Response(OK)
    }

    // throw a bunch of requests at the filter - only 5 should pass
    for (it in 1..10) {
        thread {
            println(bulkheading(Request(GET, "/")).status)
        }
    }
}
