package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.toBody
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.toHttpHandler
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestFiltersTest {

    @Test
    fun `tap passes request through to function`() {
        val get = Request(Method.GET, "")
        var called = false
        RequestFilters.Tap { called = true; assertThat(it, equalTo(get)) }.then(Response(OK).toHttpHandler())(get)
        assertTrue(called)
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