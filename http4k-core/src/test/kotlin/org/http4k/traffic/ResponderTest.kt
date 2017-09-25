package org.http4k.traffic

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.hamkrest.hasStatus
import org.junit.Test

class ResponderTest {

    private val request = Request(Method.GET, "/bob")
    private val request2 = Request(Method.GET, "/bob2")
    private val response = Response(Status.OK)

    @Test
    fun `Responder from Source replays stored responses or falls back to Service Unavailable`() {
        val cache = ReadWriteCache.Memory()
        cache[request] = response

        val responder = Responder.from(cache)
        responder(request) shouldMatch equalTo(response)
        responder(Request(Method.GET, "/rita")) shouldMatch hasStatus(SERVICE_UNAVAILABLE)
    }

    @Test
    fun `Responder from Replay replays stored responses or falls back to Service Unavailable`() {
        val stream = ReadWriteStream.Memory()
        stream[request] = response
        stream[request2] = response

        val responder = Responder.from(stream)

        responder(request) shouldMatch equalTo(response)
        responder(request2) shouldMatch equalTo(response)
        responder(Request(Method.GET, "/rita")) shouldMatch hasStatus(SERVICE_UNAVAILABLE)
    }

}