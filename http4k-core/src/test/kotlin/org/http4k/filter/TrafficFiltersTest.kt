package org.http4k.filter

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.traffic.ReadWriteCache
import org.http4k.traffic.ReadWriteStream
import org.junit.Test

class TrafficFiltersTest {
    private val request = Request(Method.GET, "/bob")
    private val response = Response(Status.OK)

    @Test
    fun `RecordTo stores traffic in underlying storage`() {
        val stream = ReadWriteStream.Memory()

        val handler = TrafficFilters.RecordTo(stream).then { response }

        handler(request) shouldMatch equalTo(response)

        stream.requests().toList() shouldMatch equalTo(listOf(request))
        stream.responses().toList() shouldMatch equalTo(listOf(response))
    }

    @Test
    fun `ServeCachedFrom serves stored requests later or falls back`() {
        val cache = ReadWriteCache.Memory()
        cache[request] = response
        val notFound = Response(Status.NOT_FOUND)
        val handler = TrafficFilters.ServeCachedFrom(cache).then { notFound }

        handler(request) shouldMatch equalTo(response)
        handler(Request(Method.GET, "/bob2")) shouldMatch equalTo(notFound)
    }

}
