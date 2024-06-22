package org.http4k.tracing

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.routing.reverseProxy
import org.http4k.strikt.bodyString
import org.http4k.strikt.status
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.RecordingEvents
import org.http4k.tracing.ActorType.Database
import org.http4k.tracing.ActorType.System
import org.http4k.tracing.tracer.HttpTracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@ExtendWith(JsonApprovalTest::class)
class TracerBulletTest {

    private val recording = RecordingEvents()
    private val events = recording
    private val child1 = Child1(events)
    private val grandchild = Grandchild(events, reverseProxy("Child1" to child1))

    private val child2 = Child2(
        events,
        reverseProxy("Grandchild" to grandchild)
    )

    private val stack = EntryPoint(
        events,
        reverseProxy("Child1" to child1, "Child2" to child2)
    )

    @Test
    fun `calls are recorded to events`(approver: Approver) {
        expectThat(Root(events, stack).call("bob")) {
            status.isEqualTo(OK)
            bodyString.isEqualTo("bob")
        }

        val traces = TracerBullet(HttpTracer(actorFrom), DbTracer(actorFrom))(recording.toList())

        approver.assertApproved(Response(OK).with(Jackson.autoBody<Any>().toLens() of traces))
        expectThat(traces).isEqualTo(listOf(expectedCallTree))
    }
}

private val actorFrom = ActorResolver {
    Actor(it.metadata["service"].toString(), System)
}

val expectedCallTree = RequestResponse(
    Actor("Root", System),
    Actor("EntryPoint", System),
    "GET {name}",
    "200 OK",
    listOf(
        RequestResponse(
            Actor("EntryPoint", System),
            Actor("Child1", System),
            "GET report",
            "200 OK",
            listOf(
                BiDirectional(
                    Actor("Child1", System),
                    Actor("db", Database),
                    "Child1",
                    listOf()
                )
            )
        ),
        BiDirectional(
            Actor("EntryPoint", System),
            Actor("db", Database),
            "EntryPoint",
            listOf()
        ),
        RequestResponse(
            Actor("EntryPoint", System),
            Actor("Child2", System),
            "GET {name}",
            "200 OK",
            listOf(
                RequestResponse(
                    Actor("Child2", System),
                    Actor("Grandchild", System),
                    "POST echo",
                    "200 OK",
                    listOf(
                        RequestResponse(
                            Actor("Grandchild", System),
                            Actor("Child1", System),
                            "GET report",
                            "200 OK",
                            listOf(
                                BiDirectional(
                                    Actor("Child1", System),
                                    Actor("db", Database),
                                    "Child1",
                                    listOf()
                                )
                            )
                        )
                    )
                )
            )
        )
    )
)
