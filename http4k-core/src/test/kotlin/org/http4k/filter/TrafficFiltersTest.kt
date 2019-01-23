package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.traffic.ReadWriteCache
import org.http4k.traffic.ReadWriteStream
import org.junit.jupiter.api.Test

class TrafficFiltersTest {
    private val request = Request(Method.GET, "/bob")
    private val response = Response(Status.OK)

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
        val notFound = Response(Status.NOT_FOUND)
        val handler = TrafficFilters.ServeCachedFrom(cache).then { notFound }

        assertThat(handler(request), equalTo(response))
        assertThat(handler(Request(Method.GET, "/bob2")), equalTo(notFound))
    }

}
