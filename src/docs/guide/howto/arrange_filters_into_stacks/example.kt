package guide.howto.arrange_filters_into_stacks

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.client.OkHttp
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.then
import org.http4k.events.Event.Companion.Error
import org.http4k.events.Events
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.RequestTracing
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.MicrometerMetrics
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.bind
import org.http4k.routing.routes

fun IncomingStack(debug: Boolean, events: Events, registry: MeterRegistry, http: HttpHandler): HttpHandler =
    RequestTracing()
        .then(if(debug) DebuggingFilters.PrintRequestAndResponse(System.out) else Filter.NoOp)
        .then(ReportHttpTransaction { events(Incoming(it)) })
        .then(CatchAll {
            events(Error("Uncaught", it))
            Response(SERVICE_UNAVAILABLE)
        })
        .then(CatchLensFailure())
        .then(ServerFilters.MicrometerMetrics.RequestTimer(registry))
        .then(ServerFilters.MicrometerMetrics.RequestCounter(registry))
        .then(http)

fun OutgoingHttpStack(debug: Boolean, events: Events, registry: MeterRegistry, http: HttpHandler) =
    RequestTracing()
        .then(if(debug) DebuggingFilters.PrintRequestAndResponse(System.out) else Filter.NoOp)
        .then(ReportHttpTransaction { events(Outgoing(it)) })
        .then(ClientFilters.MicrometerMetrics.RequestTimer(registry))
        .then(ClientFilters.MicrometerMetrics.RequestCounter(registry))
        .then(http)

fun App(debug: Boolean): HttpHandler {
    val events: Events = ::println
    val registry = SimpleMeterRegistry()

    val outgoing = OutgoingHttpStack(debug, events, registry, OkHttp())

    val endpoints = routes("/" bind outgoing)

    return IncomingStack(debug, events, registry, endpoints)
}

fun main() {
    val debug = true

    val app = App(debug)

    // this just proxies the request to the internet
    app(Request(GET, "https://www.http4k.org"))
}
