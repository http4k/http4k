package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.Test
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

class ZipkinTracesTest {

    private val requestWithTraces = Request(Method.GET, "")
        .header("x-b3-traceid", "somevalue1")
        .header("x-b3-spanid", "somevalue2")
        .header("x-b3-parentspanid", "somevalue3")

    private val expected = ZipkinTraces(TraceId("somevalue1"), TraceId("somevalue2"), TraceId("somevalue3"))

    @Test
    fun `generates a new set of traces from a request`() {
        val actual = ZipkinTraces(Request(Method.GET, ""))
        assertThat(actual, present())
        assertThat(actual.parentSpanId, absent())
    }

    @Test
    fun `gets a set of traces from a request`() {
        ZipkinTraces(requestWithTraces) shouldMatch equalTo(expected)
    }

    @Test
    fun `puts expected things onto a request`() {
        ZipkinTraces(expected, Request(Method.GET, "")) shouldMatch equalTo(requestWithTraces)
    }

}