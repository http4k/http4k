package org.http4k.connect.amazon.xray

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.xray.model.Segment
import org.http4k.connect.amazon.xray.model.SegmentId
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.connect.failureValue
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class FakeXRayTest : XRayContract, FakeAwsContract {
    private val traces = Storage.InMemory<StoredTrace>()

    override val http = FakeXRay(traces)

    private val ordered = TraceId.of("1-5759e988-bd862e3fe1be46a994272793")
    private val other = TraceId.of("1-5759e988-000000000000000000000000")

    private fun store(traceId: TraceId, at: Long, orderId: String) {
        traces[traceId.value] = StoredTrace(
            traceId = traceId,
            startTime = Timestamp.of(at),
            duration = 0.5,
            annotations = mapOf("order_id" to orderId),
            segments = listOf(Segment(SegmentId.of("0123456789abcdef"), """{"name":"checkout-api"}""")),
        )
    }

    @Test
    fun `summaries are filtered by annotation and window`() {
        store(ordered, 1000, "abc")
        store(other, 1000, "def")

        val matched = xray.getTraceSummaries(
            StartTime = Timestamp.of(900),
            EndTime = Timestamp.of(1100),
            FilterExpression = """annotation.order_id = "abc"""",
        ).successValue()

        assertThat(matched.TraceSummaries.map { it.Id }, equalTo(listOf(ordered)))
        assertThat(
            matched.TraceSummaries.single().Annotations!!.getValue("order_id")
                .single().AnnotationValue!!.StringValue,
            equalTo("abc")
        )

        val outsideWindow = xray.getTraceSummaries(Timestamp.of(1001), Timestamp.of(1100)).successValue()

        assertThat(outsideWindow.TraceSummaries, equalTo(emptyList()))
    }

    @Test
    fun `a filter expression the fake cannot evaluate is refused`() {
        assertThat(
            xray.getTraceSummaries(
                StartTime = Timestamp.of(1),
                EndTime = Timestamp.of(2),
                FilterExpression = "service(\"checkout-api\") { fault }",
            ).failureValue().status.successful,
            equalTo(false)
        )
    }

    @Test
    fun `batch get returns the stored segments and reports the rest as unprocessed`() {
        store(ordered, 1000, "abc")

        val batch = xray.batchGetTraces(listOf(ordered, other)).successValue()

        assertThat(batch.Traces.single().Segments.single().Document, equalTo("""{"name":"checkout-api"}"""))
        assertThat(batch.UnprocessedTraceIds, equalTo(listOf(other)))
    }
}
