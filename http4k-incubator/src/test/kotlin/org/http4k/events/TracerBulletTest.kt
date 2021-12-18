package org.http4k.events

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.routing.reverseProxy
import org.http4k.strikt.bodyString
import org.http4k.strikt.status
import org.http4k.testing.RecordingEvents
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant.EPOCH

class TracerBulletTest {

    private val recording = RecordingEvents()
    private val events = recording.then { println(it) }
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
    fun `calls are recorded to events`() {
        expectThat(Root(events, stack).call("bob")) {
            status.isEqualTo(OK)
            bodyString.isEqualTo("bob")
        }

        expectThat(TracerBullet(HttpTracer(::service), MyCustomTracer)(recording.toList())).isEqualTo(
            listOf(expectedCallTree)
        )
    }

    private fun service(event: MetadataEvent) = event.metadata["service"].toString()
}

val expectedCallTree = HttpTraceTree(
    "Root", Uri.of("http://EntryPoint/bob"), GET, OK,
    listOf(
        HttpTraceTree(
            "EntryPoint", Uri.of("http://Child1/report"), GET, OK,
            listOf(MyTraceTree("Child1", EPOCH, emptyList()))
        ),
        MyTraceTree("EntryPoint", EPOCH, emptyList()),
        HttpTraceTree(
            "EntryPoint", Uri.of("http://Child2/bob"), GET, OK, listOf(
                HttpTraceTree(
                    "Child2", Uri.of("http://Grandchild/echo"), POST, OK, listOf(
                        HttpTraceTree(
                            "Grandchild", Uri.of("http://Child1/report"), GET, OK, listOf(
                                MyTraceTree("Child1", EPOCH, emptyList())
                            )
                        ),
                    )
                )
            )
        )
    )
)
