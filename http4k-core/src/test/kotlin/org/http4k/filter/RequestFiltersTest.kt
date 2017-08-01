package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
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
    fun `gzip and unzip request`() {
        fun assertSupportsZipping(body: String) {
            val roundTrip = RequestFilters.GZip().then(RequestFilters.GunZip()).then { Response(OK).body(body) }
            roundTrip(Request(Method.GET, "").body(body)) shouldMatch hasBody(body)
        }
        assertSupportsZipping("foobar")
        assertSupportsZipping("")
    }
}