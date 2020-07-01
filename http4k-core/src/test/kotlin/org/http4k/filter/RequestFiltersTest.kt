package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.RequestFilters.Base64DecodeBody
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
        assertThat(
            handler(Request(GET, "http://localhost:9000/loop").header("host", "bob.com:443")),
            hasBody("http://bob.com:443/loop")
        )
        assertThat(
            handler(Request(GET, "http://localhost/loop").header("host", "bob.com")),
            hasBody("http://bob.com/loop")
        )
        assertThat(handler(Request(GET, "http://localhost:9000/loop")), hasStatus(BAD_REQUEST))
    }

    @Test
    fun `proxy host - https`() {
        val handler = RequestFilters.ProxyHost(Https).then { Response(OK).body(it.uri.toString()) }
        assertThat(
            handler(Request(GET, "http://localhost:9000/loop").header("host", "bob.com:443")),
            hasBody("https://bob.com:443/loop")
        )
        assertThat(
            handler(Request(GET, "http://localhost/loop").header("host", "bob.com")),
            hasBody("https://bob.com/loop")
        )
        assertThat(handler(Request(GET, "http://localhost:9000/loop")), hasStatus(BAD_REQUEST))
    }

    @Test
    fun `proxy host - port`() {
        val handler = RequestFilters.ProxyHost(Port).then { Response(OK).body(it.uri.toString()) }
        assertThat(
            handler(Request(GET, "http://localhost:443/loop").header("host", "bob.com")),
            hasBody("https://bob.com/loop")
        )
        assertThat(
            handler(Request(GET, "http://localhost:81/loop").header("host", "bob.com:81")),
            hasBody("http://bob.com:81/loop")
        )
        assertThat(
            handler(Request(GET, "http://localhost:80/loop").header("host", "bob.com:80")),
            hasBody("http://bob.com:80/loop")
        )
        assertThat(
            handler(Request(GET, "http://localhost/loop").header("host", "bob.com")),
            hasBody("http://bob.com/loop")
        )
        assertThat(handler(Request(GET, "http://localhost:9000/loop")), hasStatus(BAD_REQUEST))
    }

    @Test
    fun `tap passes request through to function`() {
        val get = Request(GET, "")
        var called = false
        RequestFilters.Tap { called = true; assertThat(it, equalTo(get)) }.then(Response(OK).toHttpHandler())(get)
        assertThat(called, equalTo(true))
    }

    @Test
    fun `gzip request and add content encoding`() {
        val handler = RequestFilters.GZip().then {
            assertThat(it, hasBody(equalTo(Body("foobar").gzipped().body)).and(hasHeader("content-encoding", "gzip")))
            Response(OK)
        }
        handler(Request(GET, "").body("foobar"))
    }

    @Test
    fun `gzip request and add do not content encoding where request is empty`() {
        val handler = RequestFilters.GZip().then {
            assertThat(it, hasBody(equalTo(Body.EMPTY)).and(!hasHeader("content-encoding", "gzip")))
            Response(OK)
        }
        handler(Request(GET, "").body(Body.EMPTY))
    }

    @Test
    fun `gunzip request which has gzip content encoding`() {
        fun assertSupportsUnzipping(body: String) {
            val handler = RequestFilters.GunZip().then {
                assertThat(it, hasBody(body))
                Response(OK)
            }
            handler(Request(GET, "").body(Body(body).gzipped().body).header("content-encoding", "gzip"))
        }
        assertSupportsUnzipping("foobar")
        assertSupportsUnzipping("")
    }

    @Test
    fun `passthrough gunzip request with no transfer encoding`() {
        val body = "foobar"
        val handler = ResponseFilters.GunZip().then {
            assertThat(it, hasBody(body))
            Response(OK)
        }
        handler(Request(GET, "").body(body))
    }

    @Test
    fun `base 64 decode body`() {
        val handler = Base64DecodeBody().then(RequestFilters.Assert(hasBody("hello"))).then { Response(OK) }
        handler(Request(GET, "").body("hello".base64Encode()))
    }
}
