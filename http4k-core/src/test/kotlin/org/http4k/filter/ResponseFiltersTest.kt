package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import junit.framework.TestCase.assertTrue
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.toBody
import org.http4k.core.with
import org.http4k.filter.ResponseFilters.ReportHttpTransaction
import org.http4k.filter.ResponseFilters.ReportLatency
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.lens.Header
import org.http4k.toHttpHandler
import org.http4k.util.TickingClock
import org.junit.Test
import java.time.Clock
import java.time.Clock.fixed
import java.time.Duration
import java.time.Instant.EPOCH
import java.time.ZoneId.systemDefault

class ResponseFiltersTest {

    @Test
    fun `tap passes response through to function`() {
        var called = false
        val response = Response(OK)
        ResponseFilters.Tap { called = true; assertThat(it, equalTo(response)) }.then(response.toHttpHandler())(Request(Method.GET, ""))
        assertTrue(called)
    }

    @Test
    fun `reporting latency for request`() {
        var called = false
        val request = Request(Method.GET, "")
        val response = Response(OK)

        ReportLatency(TickingClock, { req, resp, duration ->
            called = true
            assertThat(req, equalTo(request))
            assertThat(resp, equalTo(response))
            assertThat(duration, equalTo(Duration.ofSeconds(1)))
        }).then { response }(request)

        assertTrue(called)
    }

    @Test
    fun `gzip response and adds gzip content encoding if the request has accept-encoding of gzip`() {
        fun assertSupportsZipping(body: String) {
            val zipped = ResponseFilters.GZip().then { Response(OK).body(body) }
            zipped(Request(Method.GET, "").header("accept-encoding", "gzip")) shouldMatch hasBody(equalTo(body.toBody().gzipped())).and(hasHeader("content-encoding", "gzip"))
        }
        assertSupportsZipping("foobar")
        assertSupportsZipping("")
    }

    @Test
    fun `gunzip response which has gzip content encoding`() {
        fun assertSupportsUnzipping(body: String) {
            val handler = ResponseFilters.GunZip().then { Response(OK).header("content-encoding", "gzip").body(body.toBody().gzipped()) }
            handler(Request(Method.GET, "")) shouldMatch hasBody(body).and(hasHeader("content-encoding", "gzip"))
        }
        assertSupportsUnzipping("foobar")
        assertSupportsUnzipping("")
    }

    @Test
    fun `passthrough gunzip response with no content encoding when request has no accept-encoding of gzip`() {
        val body = "foobar"
        val handler = ResponseFilters.GunZip().then { Response(OK).header("content-encoding", "zip").body(body) }
        handler(Request(Method.GET, "")) shouldMatch hasBody(body).and(!hasHeader("content-encoding", "gzip"))
    }

    @Test
    fun `reporting latency for unknown route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(Clock.systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(Request(Method.GET, ""))

        assertThat(called, equalTo("GET.UNMAPPED.2xx.200"))
    }

    @Test
    fun `reporting latency for known route`() {
        var called: String? = null
        val filter = ResponseFilters.ReportRouteLatency(Clock.systemUTC(), { identity, _ -> called = identity })
        val handler = filter.then { Response(OK) }

        handler(Request(Method.GET, "").with(Header.X_URI_TEMPLATE of "/path/dir/someFile.html"))

        assertThat(called, equalTo("GET._path_dir_someFile_html.2xx.200"))
    }


    @Test
    fun `reporting http transaction for unknown route`() {
        var transaction: HttpTransaction? = null
        var called: String? = null

        val filter = ReportHttpTransaction(fixed(EPOCH, systemDefault())) { tx, identity ->
            called = identity
            transaction = tx
        }

        val handler = filter.then { Response(OK) }

        handler(Request(Method.GET, ""))

        assertThat(called, equalTo("UNMAPPED"))
        assertThat(transaction, equalTo(HttpTransaction(Request(Method.GET, ""), Response(OK), Duration.ZERO)))
    }

    @Test
    fun `reporting http transaction for known route`() {
        var transaction: HttpTransaction? = null
        var called: String? = null

        val filter = ReportHttpTransaction(fixed(EPOCH, systemDefault())) { tx, txIdentifier ->
            called = txIdentifier
            transaction = tx
        }

        val handler = filter.then { Response(OK) }

        val request = Request(Method.GET, "").with(Header.X_URI_TEMPLATE of "/path/dir/someFile.html")
        handler(request)

        assertThat(transaction, equalTo(HttpTransaction(request, Response(OK), Duration.ZERO)))
        assertThat(called, equalTo("/path/dir/someFile.html"))
    }
}