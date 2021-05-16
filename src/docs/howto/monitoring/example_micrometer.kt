package howto.monitoring

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.MicrometerMetrics
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {

    // this is a micrometer registry used mostly for testing - substitute the correct implementation.
    val registry = SimpleMeterRegistry()

    val server = routes("/metrics/{name}" bind GET to { Response(OK) })

    // apply filters to a server...
    val app = ServerFilters.MicrometerMetrics.RequestCounter(registry)
        .then(ServerFilters.MicrometerMetrics.RequestTimer(registry))
        .then(server)

    // ... or to a client
    val client =
        ClientFilters.MicrometerMetrics.RequestCounter(registry)
            .then(ClientFilters.MicrometerMetrics.RequestTimer(registry))
            .then(ApacheClient())

    // make some calls
    (0..10).forEach {
        app(Request(GET, "/metrics/$it"))
        client(Request(GET, "https://http4k.org"))
    }

    // see some results
    registry.forEachMeter { println("${it.id} ${it.measure().joinToString(",")}") }
}
