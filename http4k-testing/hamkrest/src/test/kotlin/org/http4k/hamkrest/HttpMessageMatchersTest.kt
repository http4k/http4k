package org.http4k.hamkrest

import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
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
    fun `header`() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob"), hasHeader("header", "bob"), hasHeader("header", "bill"))

    @Test
    fun `header regex`() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob"), hasHeader("header", Regex(".*bob")), hasHeader("header", Regex(".*bill")))

    @Test
    fun headers() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob").header("header", "bob2"), hasHeader("header", listOf("bob", "bob2")), hasHeader("header", listOf("bill")))

    @Test
    fun `header no value`() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob").header("header", "bob2"), hasHeader("header"), !hasHeader("header"))

    @Test
    fun `header - non-nullable string matcher`() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob").header("header", "bob2"), hasHeader("header", containsSubstring("bob")), hasHeader("header", equalTo("bill")))

    @Test
    fun `header lens`() =
        Header.required("bob").let {
            assertMatchAndNonMatch(Request(GET, "/").with(it of "bob"), hasHeader(it, equalTo("bob")), hasHeader(it, equalTo("bill")))
        }

    @Test
    fun `content type`() = assertMatchAndNonMatch(Request(GET, "/").header("Content-Type", "application/json; charset=utf-8"), hasContentType(APPLICATION_JSON), hasContentType(APPLICATION_FORM_URLENCODED))

    @Test
    fun `body string`() = assertMatchAndNonMatch(Request(GET, "/").body("bob"), hasBody("bob"), hasBody("bill"))

    @Test
    fun `body regex`() = assertMatchAndNonMatch(Request(GET, "/").body("bob"), hasBody(Regex(".*bob")), hasBody(Regex(".*bill")))

    @Test
    fun `body string matcher`() = assertMatchAndNonMatch(Request(GET, "/").body("bob"), hasBody(equalTo<Body>(Body("bob"))), hasBody(equalTo<Body>(Body("bill"))))

    @Test
    fun `body non-nullable string matcher`() = assertMatchAndNonMatch(Request(GET, "/").body("bob"), hasBody(containsSubstring("bo")), hasBody(containsSubstring("foo")))

    @Test
    fun `body matcher`() = assertMatchAndNonMatch(Request(GET, "/").body("bob"), hasBody("bob"), hasBody("bill"))

    @Test
    fun `json body matcher`() = assertMatchAndNonMatch(Request(GET, "/").body("""{"hello":"world"}"""), Jackson.hasBody("""{"hello":"world"}"""), hasBody("""{"hello":"w2orld"}"""))

    @Test
    fun `json node body matcher`() = assertMatchAndNonMatch(Request(GET, "/").body("""{"hello":"world"}"""), Jackson.hasBody(equalTo(Jackson.obj("hello" to Jackson.string("world")))), Jackson.hasBody(equalTo(Jackson.obj("hello" to Jackson.string("wo2rld")))))

    @Test
    fun `json node body equal matcher`() = assertMatchAndNonMatch(Request(GET, "/").body("""{"hello":"world"}"""), Jackson.hasBody(Jackson.obj("hello" to Jackson.string("world"))), Jackson.hasBody(Jackson.obj("hello" to Jackson.string("wo2rld"))))

    @Test
    fun `json node body equal matcher - with numbers`() = assertMatchAndNonMatch(Request(GET, "/").body("""{"hello":2}"""),
        Jackson.hasBody(Jackson.obj("hello" to Jackson.number(2))),
        Jackson.hasBody(Jackson.obj("hello" to Jackson.number(42))))

    @Test
    fun `json node body equal matcher - with longs`() = assertMatchAndNonMatch(Request(GET, "/").body("""{"hello":2}"""),
        Jackson.hasBody(Jackson.obj("hello" to Jackson.number(2L))),
        Jackson.hasBody(Jackson.obj("hello" to Jackson.number(42L))))

    @Test
    fun `body lens`() =
        Body.string(TEXT_PLAIN).toLens().let {
            assertMatchAndNonMatch(Request(GET, "/").with(it of "bob"), hasBody(it, equalTo("bob")), hasBody(it, equalTo("bill")))
        }
}
