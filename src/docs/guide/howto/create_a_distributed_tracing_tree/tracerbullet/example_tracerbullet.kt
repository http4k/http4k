package guide.howto.create_a_distributed_tracing_tree.tracerbullet

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.events.EventFilters.AddServiceName
import org.http4k.events.EventFilters.AddZipkinTraces
import org.http4k.events.Events
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.events.HttpTraceTree
import org.http4k.events.HttpTracer
import org.http4k.events.MetadataEvent
import org.http4k.events.TracerBullet
import org.http4k.events.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.ResetRequestTracing
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ServerFilters.RequestTracing
import org.http4k.routing.bind
import org.http4k.routing.reverseProxy
import org.http4k.routing.routes
import org.http4k.testing.RecordingEvents

// standardised events stack which records the service name and adds tracing
fun TraceEvents(actorName: String) = AddZipkinTraces().then(AddServiceName(actorName))

// standardised client filter stack which adds tracing and records traffic events
fun ClientStack(events: Events) = ReportHttpTransaction { events(Outgoing(it)) }
    .then(ClientFilters.RequestTracing())

// standardised server filter stack which adds tracing and records traffic events
fun ServerStack(events: Events) = ReportHttpTransaction { events(Incoming(it)) }
    .then(RequestTracing())

// Our "User" object who will send a request to our system
class User(rawEvents: Events, rawHttp: HttpHandler) {
    private val events = TraceEvents("user").then(rawEvents)

    // as the user is the initiator of requests, we need to reset the tracing for each call.
    private val http = ResetRequestTracing().then(ClientStack(events)).then(rawHttp)

    fun initiateCall() = http(Request(GET, "http://internal1/int1"))
}

// the first internal app
fun Internal1(rawEvents: Events, rawHttp: HttpHandler): HttpHandler {
    val events = TraceEvents("internal1").then(rawEvents).then(rawEvents)
    val http = ClientStack(events).then(rawHttp)

    return ServerStack(events)
        .then(
            routes("{segment}" bind { _: Request ->
                http(Request(GET, "http://external1/ext1"))
                http(Request(GET, "http://internal2/int2"))
            })
        )
}

// the second internal app
fun Internal2(rawEvents: Events, rawHttp: HttpHandler): HttpHandler {
    val events = TraceEvents("internal2").then(rawEvents).then(rawEvents)
    val http = ClientStack(events).then(rawHttp)

    return ServerStack(events)
        .then(
            routes("{segment}" bind { _: Request ->
                http(Request(GET, "http://external2/ext2"))
            })
        )
}

// an external fake system
fun FakeExternal1(): HttpHandler = { Response(OK) }

// another external fake system
fun FakeExternal2(): HttpHandler = { Response(OK) }

fun main() {
    val events = RecordingEvents()

    // compose our application(s) together
    val internalApp = Internal1(
        events,
        reverseProxy(
            "external1" to FakeExternal1(),
            "internal2" to Internal2(events, FakeExternal2())
        )
    )

    // make a request to the composed stack
    User(events, internalApp).initiateCall()

    // configure the tracerbullet
    val tracerBullet = TracerBullet(HttpTracer(::service))

    // convert the recorded events into a tree of HTTP calls
    val traceTree = tracerBullet(events.toList())

    // check that we created the correct thing
    assertThat(traceTree, equalTo(listOf(expectedCallTree)))
}

private fun service(event: MetadataEvent) = event.metadata["service"].toString()

val expectedCallTree = HttpTraceTree(
    "user", Uri.of("http://internal1/{segment}"), GET, OK,
    listOf(
        HttpTraceTree("internal1", Uri.of("http://external1/ext1"), GET, OK, emptyList()),
        HttpTraceTree(
            "internal1", Uri.of("http://internal2/{segment}"), GET, OK, listOf(
                HttpTraceTree("internal2", Uri.of("http://external2/ext2"), GET, OK, emptyList()),
            )
        )
    )
)
