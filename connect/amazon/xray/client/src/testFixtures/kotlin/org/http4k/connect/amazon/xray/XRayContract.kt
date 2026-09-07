package org.http4k.connect.amazon.xray

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.connect.failureValue
import org.http4k.connect.model.Timestamp
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.time.Instant

interface XRayContract : AwsContract {
    val xray get() = XRay.Http(aws.region, { aws.credentials }, http)

    fun traceId(seed: Int) = TraceId.of(
        "1-%08x-%s".format(Instant.now().epochSecond, uuid(seed).toString().replace("-", "").take(24))
    )

    @Test
    fun `get trace summaries over a window`() {
        val end = Instant.now()

        val summaries = xray.getTraceSummaries(
            StartTime = Timestamp.of(end.minusSeconds(60)),
            EndTime = Timestamp.of(end),
        ).successValue()

        assertThat(summaries.TraceSummaries.all { it.Id.value.startsWith("1-") }, equalTo(true))
    }

    @Test
    fun `batch get traces for ids which have no trace`() {
        val unknown = listOf(traceId(1), traceId(2))

        val traces = xray.batchGetTraces(unknown).successValue()

        assertThat(traces.Traces, equalTo(emptyList()))
        assertThat(traces.UnprocessedTraceIds.toSet(), equalTo(unknown.toSet()))
    }

    @Test
    fun `batch get traces refuses more than five ids`() {
        val tooMany = (1..6).map { traceId(it) }

        assertThat(xray.batchGetTraces(tooMany).failureValue().status.successful, equalTo(false))
    }
}
