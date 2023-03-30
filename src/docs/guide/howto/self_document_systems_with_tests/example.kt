package guide.howto.self_document_systems_with_tests

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.events.EventFilters.AddServiceName
import org.http4k.events.EventFilters.AddZipkinTraces
import org.http4k.events.Events
import org.http4k.events.HttpEvent.Incoming
import org.http4k.events.HttpEvent.Outgoing
import org.http4k.events.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.ResetRequestTracing
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ServerFilters.RequestTracing
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.reverseProxy
import org.http4k.routing.routes
import org.http4k.tracing.ActorResolver
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.junit.TracerBulletEvents
import org.http4k.tracing.persistence.FileSystem
import org.http4k.tracing.renderer.PumlSequenceDiagram
import org.http4k.tracing.tracer.HttpTracer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

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
            routes("/int1" bind { _: Request ->
                val first = http(Request(GET, "http://external1/ext1"))
                when {
                    first.status.successful -> http(Request(GET, "http://internal2/int2"))
                    else -> first
                }
            })
        )
}

// the second internal app
fun Internal2(rawEvents: Events, rawHttp: HttpHandler): HttpHandler {
    val events = TraceEvents("internal2").then(rawEvents).then(rawEvents)
    val http = ClientStack(events).then(rawHttp)

    return ServerStack(events)
        .then(
            routes("/int2" bind { _: Request ->
                http(Request(GET, "http://external2/ext2"))
            })
        )
}

// an external fake system
fun FakeExternal1(): HttpHandler = { Response(OK) }

// another external fake system
fun FakeExternal2(): HttpHandler = { Response(OK) }

private val actor = ActorResolver {
    Actor(it.metadata["service"].toString(), ActorType.System)
}

/**
 * Our test will capture the traffic and render it to the console
 */
class RenderingTest {
    @RegisterExtension
    // this events implementation will automatically capture the HTTP traffic
    val events = TracerBulletEvents(
        listOf(HttpTracer(actor)), // A tracer to capture HTTP calls
        listOf(PumlSequenceDiagram), // Render the HTTP traffic as a PUML diagram
        TraceRenderPersistence.FileSystem(File(".")) // Store the result
    )

    @Test
    fun `render successful trace`() {
        // compose our application(s) together
        val internalApp = Internal1(
            events,
            reverseProxy(
                "external1" to FakeExternal1(),
                "internal2" to Internal2(events, FakeExternal2())
            )
        )

        // make a request to the composed stack
        assertThat(User(events, internalApp).initiateCall(), hasStatus(OK))
    }

    @Test
    @Disabled("Remove this to run the failing test!")
    fun `render failure trace`() {
        // compose our application(s) together
        val internalApp = Internal1(
            events,
            reverseProxy(
                "external1" to { _: Request -> Response(INTERNAL_SERVER_ERROR) },
                "internal2" to Internal2(events, FakeExternal2())
            )
        )

        // make a request to the composed stack
        assertThat(User(events, internalApp).initiateCall(), hasStatus(OK))
    }
}
