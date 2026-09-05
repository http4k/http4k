package org.http4k.connect.amazon.xray

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.xray.model.SegmentId
import org.http4k.connect.amazon.xray.model.TimeRangeType.Event
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.connect.model.Timestamp
import org.http4k.connect.successValue
import org.http4k.core.Method.POST
import org.http4k.core.MockHttp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.http4k.hamkrest.hasUri
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HttpXRayTest {

    private val traceId = TraceId.of(TRACE_ID)

    private fun clientFor(http: MockHttp) =
        XRay.Http(Region.US_EAST_1, CredentialsProvider.FakeAwsEnvironment(), http)

    private fun uri(path: String) = Uri.of("https://xray.us-east-1.amazonaws.com$path")

    @Test
    fun `get trace summaries builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body(TRACE_SUMMARIES))

        val summaries = clientFor(mock).getTraceSummaries(
            StartTime = Timestamp.of(1614355593),
            EndTime = Timestamp.of(1614355693),
            FilterExpression = """annotation.order_id = "abc"""",
            TimeRangeType = Event,
        ).successValue()

        assertThat(mock.request!!, hasMethod(POST))
        assertThat(mock.request!!, hasUri(uri("/TraceSummaries")))
        assertThat(mock.request!!, hasHeader("Content-Type", "application/json; charset=utf-8"))
        assertThat(
            mock.request!!, hasBody(
                """{"StartTime":1614355593,"EndTime":1614355693,""" +
                    """"FilterExpression":"annotation.order_id = \"abc\"","TimeRangeType":"Event"}"""
            )
        )

        val summary = summaries.TraceSummaries.single()
        assertThat(summary.Id, equalTo(traceId))
        assertThat(summary.Duration, equalTo(0.5))
        assertThat(summary.HasError, equalTo(false))
        assertThat(
            summary.Annotations!!.getValue("order_id").single().AnnotationValue!!.StringValue,
            equalTo("abc")
        )
        assertThat(summaries.NextToken, equalTo("next"))
    }

    @Test
    fun `get trace summaries omits every optional field when not set`() {
        val mock = MockHttp(Response(OK).body("""{"TraceSummaries":[]}"""))

        clientFor(mock).getTraceSummaries(Timestamp.of(1), Timestamp.of(2)).successValue()

        assertThat(mock.request!!, hasBody("""{"StartTime":1,"EndTime":2}"""))
    }

    @Test
    fun `batch get traces builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body(TRACES))

        val traces = clientFor(mock).batchGetTraces(listOf(traceId)).successValue()

        assertThat(mock.request!!, hasMethod(POST))
        assertThat(mock.request!!, hasUri(uri("/Traces")))
        assertThat(mock.request!!, hasBody("""{"TraceIds":["$TRACE_ID"]}"""))

        val trace = traces.Traces.single()
        assertThat(trace.Id, equalTo(traceId))
        assertThat(trace.Segments.single().Id, equalTo(SegmentId.of("0123456789abcdef")))
        assertThat(trace.Segments.single().Document!!, containsSubstring("order_id"))
        assertThat(traces.UnprocessedTraceIds, equalTo(emptyList()))
    }

    @Test
    fun `requests are signed against the xray endpoint`() {
        val mock = MockHttp(Response(OK).body("""{"TraceSummaries":[]}"""))

        clientFor(mock).getTraceSummaries(Timestamp.of(1), Timestamp.of(2)).successValue()

        assertThat(mock.request!!.uri.host, equalTo("xray.us-east-1.amazonaws.com"))
        assertThat(mock.request!!.header("Authorization")!!, containsSubstring("/us-east-1/xray/aws4_request"))
    }

    @Test
    fun `trace ids and segment ids outside the AWS format are rejected`() {
        TraceId.of(TRACE_ID)
        SegmentId.of("0123456789abcdef")

        listOf("", "1-5759e988", "5759e988-bd862e3fe1be46a994272793", TRACE_ID.uppercase())
            .forEach { assertThrows<IllegalArgumentException>(it) { TraceId.of(it) } }

        listOf("", "0123456789abcde", "0123456789abcdefa")
            .forEach { assertThrows<IllegalArgumentException>(it) { SegmentId.of(it) } }
    }
}

private const val TRACE_ID = "1-5759e988-bd862e3fe1be46a994272793"

private val TRACE_SUMMARIES =
    """{"TraceSummaries":[{"Id":"$TRACE_ID","StartTime":1614355593,"Duration":0.5,"HasError":false,""" +
        """"Annotations":{"order_id":[{"AnnotationValue":{"StringValue":"abc"}}]}}],""" +
        """"TracesProcessedCount":1,"NextToken":"next"}"""

private val TRACES =
    """{"Traces":[{"Id":"$TRACE_ID","Duration":0.5,"Segments":[{"Id":"0123456789abcdef",""" +
        """"Document":"{\"name\":\"checkout-api\",\"annotations\":{\"order_id\":\"abc\"}}"}]}],""" +
        """"UnprocessedTraceIds":[]}"""
