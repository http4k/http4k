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

fun main(args: Array<String>) {

    // this is a micrometer registry used mostly for testing - substitute the correct implementation.
    val registry = SimpleMeterRegistry()

    val server = routes("/metrics" bind GET to { Response(OK) })

    val app = MetricFilters.Server.RequestCounter(registry)
        .then(MetricFilters.Server.RequestTimer(registry))
        .then(server)

    (0..10).forEach {
        app(Request(GET, "/metrics/one"))
    }

    val client =
        MetricFilters.Client.RequestCounter(registry)
            .then(MetricFilters.Client.RequestTimer(registry))
            .then(ApacheClient())

    (0..10).forEach {
        client(Request(GET, "https://http4k.org"))
    }

    registry.forEachMeter { println("${it.id} ${it.type()} ${it.measure().joinToString(",")}") }
}