package guide.reference.failsafe

import dev.failsafe.Bulkhead
import dev.failsafe.Failsafe
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.FailsafeFilter
import kotlin.concurrent.thread

fun main() {
    // Configure a Failsafe policy
    val failsafeExecutor = Failsafe.with(
        Bulkhead.of<Response>(5)
    )

    // Use the filter in a filter chain
    val app = FailsafeFilter(failsafeExecutor).then {
        Thread.sleep(100)
        Response(OK)
    }

    // Throw a bunch of requests at the filter - only 5 should pass
    for (it in 1..10) {
        thread {
            println(app(Request(GET, "/")).status)
        }
    }
}
