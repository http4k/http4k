package org.http4k.traffic

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.Test
import java.nio.file.Files

class TrafficSourceTest {

    private val request = Request(Method.GET, "/")
    private val otherRequest = Request(Method.GET, "/bob")
    private val response = Response(Status.OK).body("hello")
    private val otherResponse = Response(Status.INTERNAL_SERVER_ERROR).body("world")

    @Test
    fun `Disk ReadWriteCache can store and retrieve cached data`() {
        val tmp = Files.createTempDirectory(".").toFile()
        tmp.deleteOnExit()

        testCache(Traffic.ReadWriteCache.Disk(tmp.canonicalPath))
    }

    @Test
    fun `Memory ReadWriteCache can store and retrieve cached data`() {
        testCache(Traffic.ReadWriteCache.Memory())
    }

    @Test
    fun `Disk ReadWriteStream can store and retrieve cached data in order`() {
        testStream(Traffic.ReadWriteStream.Disk())
    }

    @Test
    fun `Memory ReadWriteStream can store and retrieve cached data in order`() {
        testStream(Traffic.ReadWriteStream.Memory())
    }

    private fun testCache(cache: Traffic.ReadWriteCache) {
        cache[request] = response
        cache[request] shouldMatch equalTo(response)
        cache[Request(Method.GET, "/bob")] shouldMatch absent()
    }

    private fun testStream(stream: Traffic.ReadWriteStream) {
        stream[request] = response
        stream[otherRequest] = otherResponse

        stream.requests().toList() shouldMatch equalTo(listOf(request, otherRequest))
        stream.responses().toList() shouldMatch equalTo(listOf(response, otherResponse))
    }

}