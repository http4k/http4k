package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.toHttpHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DebuggingFiltersTest {

    @Test
    fun `prints request and response`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "")
        val resp = Response(OK)
        PrintRequestAndResponse(PrintStream(os))
            .then(resp.toHttpHandler())(req)
        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring(req.toString()))
        assertThat(actual, containsSubstring(resp.toString()))
    }

    @Test
    fun `prints request and response when handler blows up`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "")
        try {
            PrintRequestAndResponse(PrintStream(os))
                .then { throw IllegalArgumentException("foobar") }(req)
            fail("did not throw")
        } catch (e: IllegalArgumentException) {
        }

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring(req.toString()))
        assertThat(actual, containsSubstring("foobar"))
    }

    @Test
    fun `suppresses stream body by default`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "").body("anything".byteInputStream())
        val resp = Response(OK).body("anything".byteInputStream())
        PrintRequestAndResponse(PrintStream(os))
            .then(resp.toHttpHandler())(req)
        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring(req.body("<<stream>>").toString()))
        assertThat(actual, containsSubstring(resp.body("<<stream>>").toString()))
    }

    @Test
    fun `can print stream body`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "").body("anything".byteInputStream())
        val resp = Response(OK).body("anything".byteInputStream())
        PrintRequestAndResponse(PrintStream(os), true)
            .then(resp.toHttpHandler())(req)
        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring(req.toString()))
        assertThat(actual, containsSubstring(resp.toString()))
    }
}
