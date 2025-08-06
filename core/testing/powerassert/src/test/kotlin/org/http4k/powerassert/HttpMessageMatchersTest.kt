package org.http4k.powerassert

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Header
import org.http4k.lens.string
import org.junit.jupiter.api.Test

class HttpMessageMatchersTest {

    @Test
    fun `header`() {
        val request = Request(GET, "/").header("header", "bob")
        assert(request.hasHeader("header", "bob" as String?))
        assert(!request.hasHeader("header", "bill" as String?))
    }

    @Test
    fun `header regex`() {
        val request = Request(GET, "/").header("header", "bob")
        assert(request.hasHeader("header", Regex(".*bob")))
        assert(!request.hasHeader("header", Regex(".*bill")))
    }

    @Test
    fun headers() {
        val request = Request(GET, "/").header("header", "bob").header("header", "bob2")
        assert(request.hasHeader("header", listOf("bob", "bob2")))
        assert(!request.hasHeader("header", listOf("bill")))
    }

    @Test
    fun `header no value`() {
        val request = Request(GET, "/").header("header", "bob").header("header", "bob2")
        assert(request.hasHeader("header"))
        
        val requestWithoutHeader = Request(GET, "/")
        assert(!requestWithoutHeader.hasHeader("header"))
    }

    @Test
    fun `header lens`() {
        val bobHeader = Header.required("bob")
        val requestWithHeader = Request(GET, "/").with(bobHeader of "bob")
        
        assert(requestWithHeader.hasHeader(bobHeader, "bob"))
        assert(!requestWithHeader.hasHeader(bobHeader, "bill"))
    }

    @Test
    fun `content type`() {
        val request = Request(GET, "/").header("Content-Type", "application/json; charset=utf-8")
        assert(request.hasContentType(APPLICATION_JSON))
        assert(!request.hasContentType(APPLICATION_FORM_URLENCODED))
    }

    @Test
    fun `body string`() {
        val request = Request(GET, "/").body("bob")
        assert(request.hasBody("bob" as String?))
        assert(!request.hasBody("bill" as String?))
    }

    @Test
    fun `body regex`() {
        val request = Request(GET, "/").body("bob")
        assert(request.hasBody(Regex(".*bob")))
        assert(!request.hasBody(Regex(".*bill")))
    }

    @Test
    fun `body object`() {
        val request = Request(GET, "/").body("bob")
        assert(request.hasBody(Body("bob")))
        assert(!request.hasBody(Body("bill")))
    }

    @Test
    fun `json body matcher`() {
        val request = Request(GET, "/").body("""{"hello":"world"}""")
        assert(Jackson.hasBody(request, """{"hello":"world"}"""))
        assert(!Jackson.hasBody(request, """{"hello":"w2orld"}"""))
    }

    @Test
    fun `json node body equal matcher`() {
        val request = Request(GET, "/").body("""{"hello":"world"}""")
        assert(Jackson.hasBody(request, Jackson.obj("hello" to Jackson.string("world"))))
        assert(!Jackson.hasBody(request, Jackson.obj("hello" to Jackson.string("wo2rld"))))
    }

    @Test
    fun `json node body equal matcher - with numbers`() {
        val request = Request(GET, "/").body("""{"hello":2}""")
        assert(Jackson.hasBody(request, Jackson.obj("hello" to Jackson.number(2))))
        assert(!Jackson.hasBody(request, Jackson.obj("hello" to Jackson.number(42))))
    }

    @Test
    fun `json node body equal matcher - with longs`() {
        val request = Request(GET, "/").body("""{"hello":2}""")
        assert(Jackson.hasBody(request, Jackson.obj("hello" to Jackson.number(2L))))
        assert(!Jackson.hasBody(request, Jackson.obj("hello" to Jackson.number(42L))))
    }

    @Test
    fun `body lens`() {
        val bodyLens = Body.string(TEXT_PLAIN).toLens()
        val requestWithBody = Request(GET, "/").with(bodyLens of "bob")
        
        assert(requestWithBody.hasBody(bodyLens, "bob"))
        assert(!requestWithBody.hasBody(bodyLens, "bill"))
    }
}