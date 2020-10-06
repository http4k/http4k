package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.filter.SamplingDecision.Companion.DO_NOT_SAMPLE
import org.http4k.filter.SamplingDecision.Companion.SAMPLE
import org.junit.jupiter.api.Test
import kotlin.random.Random


class TraceIdTest {

    @Test
    fun `creates a new random`() {
        val r = Random(1)
        assertThat(TraceId.new(r), equalTo(TraceId("1a2ac523b005b1a4")))
        assertThat(TraceId.new(r), equalTo(TraceId("6d33b6a05a4abadf")))
    }
}

class SamplingDecisionTest {

    @Test
    fun `accepts valid values`() {
        assertThat(SamplingDecision.from("1"), equalTo(SAMPLE))
        assertThat(SamplingDecision.from("0"), equalTo(DO_NOT_SAMPLE))
    }

    @Test
    fun `defaults to sample`() {
        assertThat(SamplingDecision.from(null), equalTo(SAMPLE))
    }

    @Test
    fun `parses invalid values as sample`() {
        assertThat(SamplingDecision.from("true"), equalTo(SAMPLE))
        assertThat(SamplingDecision.from("false"), equalTo(SAMPLE))
        assertThat(SamplingDecision.from("wibble"), equalTo(SAMPLE))
    }
}

class ZipkinTracesTest {

    private val requestWithTraces = Request(GET, "")
        .header("x-b3-traceid", "somevalue1")
        .header("x-b3-spanid", "somevalue2")
        .header("x-b3-parentspanid", "somevalue3")

    private val expectedWithTraces = ZipkinTraces(TraceId("somevalue1"), TraceId("somevalue2"), TraceId("somevalue3"), SAMPLE)

    private val requestWithDecision = Request(GET, "")
        .header("x-b3-traceid", "somevalue1")
        .header("x-b3-spanid", "somevalue2")
        .header("x-b3-parentspanid", "somevalue3")
        .header("x-b3-sampled", "0")

    private val expectedWithDecision = ZipkinTraces(TraceId("somevalue1"), TraceId("somevalue2"), TraceId("somevalue3"), DO_NOT_SAMPLE)

    @Test
    fun `generates a new set of traces from a request`() {
        val actual = ZipkinTraces(Request(GET, ""))
        assertThat(actual, present())
        assertThat(actual.parentSpanId, absent())
        assertThat(actual.samplingDecision, equalTo(SAMPLE))
    }

    @Test
    fun `gets a set of traces from a request without a sampling decision`() {
        assertThat(ZipkinTraces(requestWithTraces), equalTo(expectedWithTraces))
    }

    @Test
    fun `gets a set of traces from a request with a sampling decision`() {
        assertThat(ZipkinTraces(requestWithDecision), equalTo(expectedWithDecision))
    }

    @Test
    fun `puts expected things onto a request`() {
        val requestWithDefaultDecision = requestWithTraces.header("x-b3-sampled", "1")
        assertThat(ZipkinTraces(expectedWithTraces, Request(GET, "")), equalTo(requestWithDefaultDecision))
    }

    @Test
    fun `puts expected things onto a request with a sampling decision`() {
        assertThat(ZipkinTraces(expectedWithDecision, Request(GET, "")), equalTo(requestWithDecision))
    }
}
