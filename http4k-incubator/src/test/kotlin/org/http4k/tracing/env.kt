package org.http4k.tracing

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.events.EventFilters.AddServiceName
import org.http4k.events.EventFilters.AddTimestamp
import org.http4k.events.EventFilters.AddZipkinTraces
import org.http4k.events.Events
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.events.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.ResetRequestTracing
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ServerFilters
import org.http4k.filter.ZipkinTracesStorage
import org.http4k.filter.ZipkinTracesStorage.Companion.THREAD_LOCAL
import org.http4k.filter.inChildSpan
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.util.TickingClock
import java.time.Instant.EPOCH

class Root(rawEvents: Events, http: HttpHandler) {
    private val events = TraceEvents("Root").then(rawEvents)

    private val http = ResetRequestTracing().then(ClientStack(events)).then(http)

    fun call(name: String) = http(Request(GET, "http://EntryPoint/$name"))
}

private val clock = TickingClock()

fun TraceEvents(actorName: String) =
    AddZipkinTraces().then(AddServiceName(actorName)).then(AddTimestamp(clock))

fun ClientStack(events: Events) = ClientFilters.RequestTracing()
    .then(ReportHttpTransaction { events(Outgoing(it)) })

fun ServerStack(events: Events) = ServerFilters.RequestTracing()
    .then(ReportHttpTransaction { events(Incoming(it)) })

fun Database(events: Events, tracesStorage: ZipkinTracesStorage = THREAD_LOCAL): (String) -> Unit = { caller: String ->
    tracesStorage.inChildSpan {
        events(DbEvent(caller, EPOCH))
    }
}

fun EntryPoint(rawEvents: Events, http: HttpHandler): HttpHandler {
    val events = TraceEvents("EntryPoint").then(rawEvents)
    val client = ClientStack(events).then(http)
    val database = Database(events)

    return ServerStack(events).then(
        routes("{name}" bind GET to { req: Request ->
            client(Request(GET, "http://Child1/report"))
            database("EntryPoint")
            client(Request(GET, "http://Child2/" + req.path("name")!!))
        })
    )
}

fun Child1(rawEvents: Events): HttpHandler {
    val events = TraceEvents("Child1").then(rawEvents)
    val database = Database(events)

    return ServerStack(events).then(
        routes("report" bind GET to { _: Request ->
            database("Child1")
            Response(OK)
        })
    )
}

fun Child2(rawEvents: Events, http: HttpHandler): HttpHandler {
    val events = TraceEvents("Child2").then(rawEvents)
    val client = ClientStack(events).then(http)
    return ServerStack(events).then(
        routes("{name}" bind GET to { req: Request ->
            client(Request(POST, "http://Grandchild/echo").body(req.path("name")!!))
        })
    )
}

fun Grandchild(rawEvents: Events, child1: HttpHandler): HttpHandler {
    val events = TraceEvents("Grandchild").then(rawEvents)
    val client = ClientStack(events).then(child1)
    return ServerStack(events).then(
        routes("echo" bind POST to { req: Request ->
            client(Request(GET, "http://Child1/report"))
            Response(OK).body(req.body)
        })
    )
}
