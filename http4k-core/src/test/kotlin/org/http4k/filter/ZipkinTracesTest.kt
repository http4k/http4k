package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.filter.SamplingDecision.Companion.DO_NOT_SAMPLE
import org.http4k.filter.SamplingDecision.Companion.SAMPLE
import org.junit.jupiter.api.Test
import java.util.Random


class TraceIdTest {

    @Test
    fun `creates a new random`() {
        val r = Random()
        r.setSeed(1)
        TraceId.new(r) shouldMatch equalTo(TraceId("73d51abbd89cb819"))
        TraceId.new(r) shouldMatch equalTo(TraceId("6f0efb6892f94d68"))
    }

}

class SamplingDecisionTest {

    @Test
    fun `accepts valid values`() {
        SamplingDecision.from("1") shouldMatch equalTo(SAMPLE)
        SamplingDecision.from("0") shouldMatch equalTo(DO_NOT_SAMPLE)
    }

    @Test
    fun `defaults to sample`() {
        SamplingDecision.from(null) shouldMatch equalTo(SAMPLE)
    }

    @Test
    fun `parses invalid values as sample`() {
        SamplingDecision.from("true") shouldMatch equalTo(SAMPLE)
        SamplingDecision.from("false") shouldMatch equalTo(SAMPLE)
        SamplingDecision.from("wibble") shouldMatch equalTo(SAMPLE)
    }

}

class ZipkinTracesTest {

    private val requestWithTraces = Request(Method.GET, "")
            .header("x-b3-traceid", "somevalue1")
            .header("x-b3-spanid", "somevalue2")
            .header("x-b3-parentspanid", "somevalue3")

    private val expectedWithTraces = ZipkinTraces(TraceId("somevalue1"), TraceId("somevalue2"), TraceId("somevalue3"), SAMPLE)

    private val requestWithDecision = Request(Method.GET, "")
            .header("x-b3-traceid", "somevalue1")
            .header("x-b3-spanid", "somevalue2")
            .header("x-b3-parentspanid", "somevalue3")
            .header("x-b3-sampled", "0")

    private val expectedWithDecision = ZipkinTraces(TraceId("somevalue1"), TraceId("somevalue2"), TraceId("somevalue3"), DO_NOT_SAMPLE)

    @Test
    fun `generates a new set of traces from a request`() {
        val actual = ZipkinTraces(Request(Method.GET, ""))
        assertThat(actual, present())
        assertThat(actual.parentSpanId, absent())
        assertThat(actual.samplingDecision, equalTo(SAMPLE))
    }

    @Test
    fun `gets a set of traces from a request without a sampling decision`() {
        ZipkinTraces(requestWithTraces) shouldMatch equalTo(expectedWithTraces)
    }

    @Test
    fun `gets a set of traces from a request with a sampling decision`() {
        ZipkinTraces(requestWithDecision) shouldMatch equalTo(expectedWithDecision)
    }

    @Test
    fun `puts expected things onto a request`() {
        val requestWithDefaultDecision = requestWithTraces.header("x-b3-sampled", "1")
        ZipkinTraces(expectedWithTraces, Request(Method.GET, "")) shouldMatch equalTo(requestWithDefaultDecision)
    }

    @Test
    fun `puts expected things onto a request with a sampling decision`() {
        ZipkinTraces(expectedWithDecision, Request(Method.GET, "")) shouldMatch equalTo(requestWithDecision)
    }

}