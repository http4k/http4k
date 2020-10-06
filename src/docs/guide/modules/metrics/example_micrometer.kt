package guide.modules.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.MetricFilters
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {

    // this is a micrometer registry used mostly for testing - substitute the correct implementation.
    val registry = SimpleMeterRegistry()

    val server = routes("/metrics" bind GET to { Response(OK) })

    // apply filters to a server...
    val app = MetricFilters.Server.RequestCounter(registry)
        .then(MetricFilters.Server.RequestTimer(registry))
        .then(server)

    // ... or to a client
    val client =
        MetricFilters.Client.RequestCounter(registry)
            .then(MetricFilters.Client.RequestTimer(registry))
            .then(ApacheClient())

    // make some calls
    (0..10).forEach {
        app(Request(GET, "/metrics"))
        client(Request(GET, "https://http4k.org"))
    }

    // see some results
    registry.forEachMeter { println("${it.id} ${it.measure().joinToString(",")}") }
}
