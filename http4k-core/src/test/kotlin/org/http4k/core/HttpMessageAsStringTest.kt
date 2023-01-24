package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class HttpMessageAsStringTest {

    @Test
    fun `represents request as string`() {
        val request = Request(GET, Uri.of("http://www.somewhere.com/path"))
            .header("foo", "one").header("bar", "two").body(Body("body"))
        assertThat(request.toString(), equalTo("""
            GET http://www.somewhere.com/path HTTP/1.1
            foo: one
            bar: two

            body""".toPayload()))
    }

    @Test
    fun `represents response as string`() {
        val request = Response(OK)
            .header("foo", "one").header("bar", "two").body("body")
        assertThat(request.toString(), equalTo("""
            HTTP/1.1 200 OK
            foo: one
            bar: two

            body""".toPayload()))
    }

    @Test
    fun `parses request string`() {
        assertThat(Request.parse("""GET http://www.somewhere.com/path HTTP/1.1
foo:one
bar: two
baz:  three

body""".toPayload()), equalTo(Request(GET, Uri.of("http://www.somewhere.com/path"))
            .header("foo", "one")
            .header("bar", "two")
            .header("baz", "three")
            .body(Body("body"))
        ))
    }

    @Test
    fun `parses response string`() {
        assertThat(Response.parse("""
HTTP/1.1 200 OK
foo:one
bar: two
baz:  three

body""".toPayload()), equalTo(Response(OK)
            .header("foo", "one")
            .header("bar", "two")
            .header("baz", "three")
            .body("body")
        ))
    }

    @Test
    fun `parse response with other status`() {
        assertThat(Response.parse(Response(NOT_FOUND).body("hi").toString()),
            equalTo(Response(NOT_FOUND).body("hi")))
    }

    @Test
    fun `parse status without description`() {
        assertThat(Response.parse("""
HTTP/1.1 200

""".toPayload()), equalTo(Response(Status(200, ""))))
    }

    @Test
    fun `cannot parse empty request`() {
        assertParsingFailure({ Request.parse("") }, "Empty message")
    }

    @Test
    fun `cannot invalid request method`() {
        assertParsingFailure({ Request.parse("FLY away") }, "Invalid method: FLY")
    }

    @Test
    fun `cannot parse invalid request line`() {
        assertParsingFailure({ Request.parse("GET") }, "Invalid request line: GET")
    }

    @Test
    fun `cannot parse empty response`() {
        assertParsingFailure({ Response.parse("") }, "Empty message")
    }

    @Test
    fun `cannot parse invalid response status code`() {
        assertParsingFailure({ Response.parse("HTTP/1.1 nothing to report") }, "Invalid HTTP status: nothing")
    }

    @Test
    fun `parse using other character as line break`() {
        assertThat(Request.parse("""GET http://www.somewhere.com/path HTTP/1.1,,body""", ","),
            equalTo(Request(GET, Uri.of("http://www.somewhere.com/path")).body(Body("body"))
        ))
    }

    private fun assertParsingFailure(action: () -> Any, message: String) {
        try {
            action()
            fail("should have failed")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message, equalTo(message))
        }
    }

    private fun String.toPayload() = trimIndent().replace("\n", "\r\n")
}
