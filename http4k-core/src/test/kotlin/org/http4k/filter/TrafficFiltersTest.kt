package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.traffic.ReadWriteCache
import org.http4k.traffic.ReadWriteStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class TrafficFiltersTest {
    private val request = Request(GET, "/bob")
    private val response = Response(OK)

    @Test
    fun `RecordTo stores traffic in underlying storage`() {
        val stream = ReadWriteStream.Memory()

        val handler = TrafficFilters.RecordTo(stream).then { response }

        assertThat(handler(request), equalTo(response))

        assertThat(stream.requests().toList(), equalTo(listOf(request)))
        assertThat(stream.responses().toList(), equalTo(listOf(response)))
    }

    @Test
    fun `ServeCachedFrom serves stored requests later or falls back`() {
        val cache = ReadWriteCache.Memory()
        cache[request] = response
        val notFound = Response(NOT_FOUND)
        val handler = TrafficFilters.ServeCachedFrom(cache).then { notFound }

        assertThat(handler(request), equalTo(response))
        assertThat(handler(Request(GET, "/bob2")), equalTo(notFound))
    }

    @Test
    fun `ReplayFrom serves stored requests later or returns 400`() {
        val cache = ReadWriteStream.Memory()
        cache[request] = response
        cache[request] = response
        val handler = TrafficFilters.ReplayFrom(cache).then { fail("") }

        assertThat(handler(request), equalTo(response))
        assertThat(handler(request), equalTo(response))
        assertThat(handler(Request(GET, "/bob2")), equalTo(Response(BAD_REQUEST)))
    }
}
