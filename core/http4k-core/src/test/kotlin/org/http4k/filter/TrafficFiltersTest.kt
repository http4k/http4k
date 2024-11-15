package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.hamkrest.hasBody
import org.http4k.server.SunHttp
import org.http4k.server.asServer
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

        val handler = RecordTo(stream).then { response }

        assertThat(handler(request), equalTo(response))

        assertThat(stream.requests().toList(), equalTo(listOf(request)))
        assertThat(stream.responses().toList(), equalTo(listOf(response)))
    }

    @Test
    fun `RecordTo stores traffic in underlying storage on server`() {
        val stream = ReadWriteStream.Memory()

        val request1 = Request(POST, "").body("helloworld")
        val request2 = Request(POST, "").body("goodbyeworld")

        RecordTo(stream)
            .then { responseFor(it) }
            .asServer(SunHttp(0))
            .start().use {
                val client = SetBaseUriFrom(Uri.of("http://localhost:${it.port()}"))
                    .then(JavaHttpClient())
                assertThat(client(request1), hasBody("helloworld".reversed()))
                assertThat(client(request2), hasBody("goodbyeworld".reversed()))
            }

        assertThat(stream.requests().toList()[0], hasBody(request1.bodyString()))
        assertThat(stream.requests().toList()[1], hasBody(request2.bodyString()))
        assertThat(stream.responses().toList()[0], hasBody(responseFor(request1).bodyString()))
        assertThat(stream.responses().toList()[1], hasBody(responseFor(request2).bodyString()))
    }

    private fun responseFor(req: Request) =
        Response(OK).body(req.body.stream.reader().readText().reversed().byteInputStream())

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
        cache[Request(GET, "/bob1")] = Response(OK)
        cache[Request(GET, "/bob2")] = Response(ACCEPTED)
        cache[Request(GET, "/bob3")] = Response(NOT_FOUND)
        val handler = TrafficFilters.ReplayFrom(cache).then { fail("") }

        assertThat(handler(Request(GET, "/bob1")), equalTo(Response(OK)))
        assertThat(handler(Request(GET, "/bob2")), equalTo(Response(ACCEPTED)))
        assertThat(handler(Request(GET, "/bob3")), equalTo(Response(NOT_FOUND)))
        assertThat(handler(Request(GET, "/bob2")), equalTo(Response(BAD_REQUEST)))
    }
}
