package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.toBody
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Http
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Https
import org.http4k.filter.RequestFilters.ProxyProtocolMode.Port
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.toHttpHandler
import org.junit.jupiter.api.Test

class RequestFiltersTest {
    @Test
    fun `proxy host - http`() {
        val handler = RequestFilters.ProxyHost(Http).then { Response(OK).body(it.uri.toString()) }
        handler(Request(GET, "http://localhost:9000/loop").header("host", "bob.com:443")) shouldMatch hasBody("http://bob.com:443/loop")
        handler(Request(GET, "http://localhost/loop").header("host", "bob.com")) shouldMatch hasBody("http://bob.com/loop")
        handler(Request(GET, "http://localhost:9000/loop")) shouldMatch hasStatus(Status.BAD_REQUEST)
    }

    @Test
    fun `proxy host - https`() {
        val handler = RequestFilters.ProxyHost(Https).then { Response(OK).body(it.uri.toString()) }
        handler(Request(GET, "http://localhost:9000/loop").header("host", "bob.com:443")) shouldMatch hasBody("https://bob.com:443/loop")
        handler(Request(GET, "http://localhost/loop").header("host", "bob.com")) shouldMatch hasBody("https://bob.com/loop")
        handler(Request(GET, "http://localhost:9000/loop")) shouldMatch hasStatus(Status.BAD_REQUEST)
    }

    @Test
    fun `proxy host - port`() {
        val handler = RequestFilters.ProxyHost(Port).then { Response(OK).body(it.uri.toString()) }
        handler(Request(GET, "http://localhost:443/loop").header("host", "bob.com")) shouldMatch hasBody("https://bob.com/loop")
        handler(Request(GET, "http://localhost:81/loop").header("host", "bob.com:81")) shouldMatch hasBody("http://bob.com:81/loop")
        handler(Request(GET, "http://localhost:80/loop").header("host", "bob.com:80")) shouldMatch hasBody("http://bob.com:80/loop")
        handler(Request(GET, "http://localhost/loop").header("host", "bob.com")) shouldMatch hasBody("http://bob.com/loop")
        handler(Request(GET, "http://localhost:9000/loop")) shouldMatch hasStatus(Status.BAD_REQUEST)
    }

    @Test
    fun `tap passes request through to function`() {
        val get = Request(Method.GET, "")
        var called = false
        RequestFilters.Tap { called = true; assertThat(it, equalTo(get)) }.then(Response(OK).toHttpHandler())(get)
        assertThat(called, equalTo(true))
    }

    @Test
    fun `gzip request and add content encoding`() {
        fun assertSupportsZipping(body: String) {
            val handler = RequestFilters.GZip().then {
                it shouldMatch hasBody(equalTo(body.toBody().gzipped())).and(hasHeader("content-encoding", "gzip"))
                Response(OK)
            }
            handler(Request(Method.GET, "").body(body))
        }
        assertSupportsZipping("foobar")
        assertSupportsZipping("")
    }

    @Test
    fun `gunzip request which has gzip content encoding`() {
        fun assertSupportsUnzipping(body: String) {
            val handler = RequestFilters.GunZip().then {
                it shouldMatch hasBody(body)
                Response(OK)
            }
            handler(Request(Method.GET, "").body(body.toBody().gzipped()).header("content-encoding", "gzip"))
        }
        assertSupportsUnzipping("foobar")
        assertSupportsUnzipping("")
    }

    @Test
    fun `passthrough gunzip request with no transfer encoding`() {
        val body = "foobar"
        val handler = ResponseFilters.GunZip().then {
            it shouldMatch hasBody(body)
            Response(OK)
        }
        handler(Request(Method.GET, "").body(body))
    }

}