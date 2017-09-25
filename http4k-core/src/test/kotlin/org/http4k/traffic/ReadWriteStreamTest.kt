package org.http4k.traffic

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.Test
import java.nio.file.Files

class ReadWriteStreamTest {

    @Test
    fun `Disk ReadWriteStream can store and retrieve cached data in order`() {
        val tmp = Files.createTempDirectory(".").toFile()
        tmp.deleteOnExit()

        testStream(ReadWriteStream.Disk(tmp.absolutePath))
    }

    @Test
    fun `Memory ReadWriteStream can store and retrieve cached data in order`() {
        testStream(ReadWriteStream.Memory())
    }

    private fun testStream(stream: ReadWriteStream) {
        val request = Request(Method.GET, "/")
        val otherRequest = Request(Method.GET, "/bob")
        val response = Response(Status.OK).body("hello")
        val otherResponse = Response(Status.INTERNAL_SERVER_ERROR).body("world")
        stream[request] = response
        stream[otherRequest] = otherResponse

        stream.requests().toList() shouldMatch equalTo(listOf(request, otherRequest))
        stream.responses().toList() shouldMatch equalTo(listOf(response, otherResponse))
    }

}