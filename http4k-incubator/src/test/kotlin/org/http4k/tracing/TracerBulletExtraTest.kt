package org.http4k.tracing

import org.http4k.events.MetadataEvent
import org.http4k.events.MyEvent
import org.http4k.filter.TraceId
import org.http4k.filter.ZipkinTraces
import org.http4k.format.Jackson
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class TracerBulletExtraTest {

    private val events = listOf(
        eventWith("Event 1.1 (A-B)", "1", "span1.B.1", "span1.A.1"),
        eventWith("Event 1.2 (A-B)", "1", "span1.B.2", "span1.A.1"),
        eventWith("Event 1.5 (C-D)", "1", "span1.D.1", "span1.C.1"),
        eventWith("Event 1.4 (B-C)", "1", "span1.C.1", "span1.B.3"),
        eventWith("Event 1.3 (A-B)", "1", "span1.B.3", "span1.A.1"),
        eventWith("Event 1.8 (C-D)", "1", "span1.D.2", "span1.C.2"),
        eventWith("Event 1.7 (B-C)", "1", "span1.C.2", "span1.B.4"),
        eventWith("Event 1.6 (A-B)", "1", "span1.B.4", "span1.A.1"),
        eventWith("Event 1.9 (A-D)", "1", "span1.D.3", "span1.A.1"),
        eventWith("Event 1.0", "-A", "span1.A.1", null),
        eventWith("Event 2.1 (A-B)", "2", "span2.B.1", "span2.A.1"),
        eventWith("Event 2.2 (A-B)", "2", "span2.B.2", "span2.A.1"),
        eventWith("Event 2.5 (D-E)", "2", "span2.E.1", "span2.D.1"),
        eventWith("Event 2.4 (B-D)", "2", "span2.D.1", "span2.B.3"),
        eventWith("Event 2.3 (A-B)", "2", "span2.B.3", "span2.A.1"),
        eventWith("Event 2.6 (A-E)", "2", "span2.E.2", "span2.A.1"),
        eventWith("Event 2.0", "-B", "span2.A.1", null)
    )

    private var time = 0

    private fun eventWith(descrition: String, traceId: String, span: String, parentSpan: String?) =
        MetadataEvent(
            MyEvent(descrition), mapOf("traces" to trace(traceId, span, parentSpan), "timestamp" to time++)
        )

    @Test
    fun `traces can be grouped`(approver: Approver) {
        val input = events.buildTree()
        prettyPrint(input, 0)
        approver.assertApproved(Jackson.prettify(Jackson.asFormatString(input)))
    }
}

private fun prettyPrint(eventNodes: List<EventNode>, level: Int) {
    for (eventNode in eventNodes) {
        repeat(level) { print("  ") }
        println(eventNode.event.event)
        prettyPrint(eventNode.children, level + 1)
    }
}

fun trace(trace: String, span: String, parent: String?) =
    ZipkinTraces(TraceId(trace), TraceId(span), parent?.let(::TraceId))
