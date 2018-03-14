package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import junit.framework.TestCase.assertTrue
import org.http4k.core.HttpTransaction
import org.http4k.core.HttpTransaction.Companion.ROUTING_GROUP_LABEL
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.toBody
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ResponseFilters.ReportLatency
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.toHttpHandler
import org.http4k.util.TickingClock
import org.junit.Test
import java.time.Clock
import java.time.Clock.fixed
import java.time.Duration.ZERO
import java.time.Duration.ofSeconds
import java.time.Instant.EPOCH
import java.time.ZoneId.systemDefault

class ResponseFiltersTest {

    @Test
    fun `tap passes response through to function`() {
        var called = false
        val response = Response(OK)
        ResponseFilters.Tap { called = true; assertThat(it, equalTo(response)) }.then(response.toHttpHandler())(Request(GET, ""))
        assertTrue(called)
    }

    @Test
    fun `reporting latency for request`() {
        var called = false
        val request = Request(GET, "")
        val response = Response(OK)

        ReportLatency(TickingClock, { req, resp, duration ->
            called = true
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(ofSeconds(1)))
        }).then { response }(request)

        assertTrue(called)
    }

    @Test
    fun `gzip response and adds gzip content encoding if the request has accept-encoding of gzip`() {
        fun assertSupportsZipping(body: String) {
            val zipped = ResponseFilters.GZip().then { Response(OK).body(body) }
            zipped(Request(GET, "").header("accept-encoding", "gzip")) shouldMatch hasBody(equalTo(body.toBody().gzipped())).and(hasHeader("content-encoding", "gzip"))
        }
        assertSupportsZipping("foobar")
        assertSupportsZipping("")
    }

    @Test
    fun `gunzip response which has gzip content encoding`() {
        fun assertSupportsUnzipping(body: String) {
            val handler = ResponseFilters.GunZip().then { Response(OK).header("content-encoding", "gzip").body(body.toBody().gzipped()) }
            handler(Request(GET, "")) shouldMatch hasBody(body).and(hasHeader("content-encoding", "gzip"))
        }
        assertSupportsUnzipping("foobar")
        assertSupportsUnzipping("")
    }

    @Test
    fun `passthrough gunzip response with no content encoding when request has no accept-encoding of gzip`() {
        val body = "foobar"
        val handler = ResponseFilters.GunZip().then { Response(OK).header("content-encoding", "zip").body(body) }
        handler(Request(GET, "")) shouldMatch hasBody(body).and(!hasHeader("content-encoding", "gzip"))
    }

    @Test
    fun `reporting latency for unknown route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(Clock.systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(Request(GET, ""))

        assertThat(called, equalTo("GET.UNMAPPED.2xx.200"))
    }

    @Test
    fun `reporting latency for known route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(Clock.systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then(routes("/bob/{anything:.*}" bind GET to { Response(OK) }))

        handler(Request(GET, "/bob/dir/someFile.html"))

        assertThat(called, equalTo("GET.bob_{anything._*}.2xx.200"))
    }

    @Test
    fun `reporting http transaction for unknown route`() {
        var transaction: HttpTransaction? = null

        val filter = ReportHttpTransaction(fixed(EPOCH, systemDefault())) { transaction = it }

        val handler = filter.then { Response(OK) }

        val request = Request(GET, "")

        handler(request)

        assertThat(transaction, equalTo(HttpTransaction(request, Response(OK), ZERO, emptyMap())))
    }

    @Test
    fun `reporting http transaction for known route`() {
        var transaction: HttpTransaction? = null

        val filter = ReportHttpTransaction(fixed(EPOCH, systemDefault())) {
            transaction = it
        }

        val handler = filter.then(
            routes("/sue" bind routes("/bob/{name}" bind GET to { Response(OK) }))
        )

        val request = Request(GET, "/sue/bob/rita")

        handler(request)

        assertThat(transaction, equalTo(HttpTransaction(request,
            Response(OK), ZERO, mapOf(ROUTING_GROUP_LABEL to "sue/bob/{name}"))))
    }
}