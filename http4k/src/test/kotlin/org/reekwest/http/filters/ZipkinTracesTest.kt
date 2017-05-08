package org.http4k.http.filters

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.Test
import org.http4k.http.core.Request.Companion.get
import java.util.*


class TraceIdTest {

    @Test
    fun `creates a new random`() {
        val r = Random()
        r.setSeed(1)
        assertThat(TraceId.new(r), equalTo(TraceId("73d51abbd89cb819")))
        assertThat(TraceId.new(r), equalTo(TraceId("6f0efb6892f94d68")))
    }

}

class ZipkinTracesTest {

    private val requestWithTraces = get("")
        .header("x-b3-traceid", "somevalue1")
        .header("x-b3-spanid", "somevalue2")
        .header("x-b3-parentspanid", "somevalue3")

    private val expected = ZipkinTraces(TraceId("somevalue1"), TraceId("somevalue2"), TraceId("somevalue3"))

    @Test
    fun `generates a new set of traces from a request`() {
        val actual = ZipkinTraces(get(""))
        assertThat(actual, present())
        assertThat(actual.parentSpanId, absent())
    }

    @Test
    fun `gets a set of traces from a request`() {
        assertThat(ZipkinTraces(requestWithTraces),
            equalTo(expected))
    }

    @Test
    fun `puts expected things onto a request`() {
        assertThat(ZipkinTraces(expected, get("")), equalTo(requestWithTraces))
    }

}