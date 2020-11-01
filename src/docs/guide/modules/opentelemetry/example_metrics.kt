package guide.modules.opentelemetry

import io.opentelemetry.exporters.inmemory.InMemoryMetricExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.metrics.data.MetricData
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.OpenTelemetryMetrics
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes

fun main() {
    val server = routes("/metrics" bind GET to { Response(OK) })

    // apply metrics filters to a server...
    val app = ServerFilters.OpenTelemetryMetrics.RequestCounter()
        .then(ServerFilters.OpenTelemetryMetrics.RequestTimer())
        .then(server)

    // ... or to a client
    val client =
        ClientFilters.OpenTelemetryMetrics.RequestCounter()
            .then(ClientFilters.OpenTelemetryMetrics.RequestTimer())
            .then(ApacheClient())

    // make some calls
    repeat(5) {
        app(Request(GET, "/metrics"))
        client(Request(GET, "https://http4k.org"))
    }

    // see some results
    exportMetricsFromOpenTelemetry().forEach {
        println("metric: " + it.name + ", value: " + it.points)
    }
}

private fun exportMetricsFromOpenTelemetry(): List<MetricData> = InMemoryMetricExporter.create().apply {
    export(OpenTelemetrySdk.getMeterProvider().metricProducer.collectAllMetrics())
}.finishedMetricItems
