package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.be
import io.kotest.matchers.string.contain
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
    fun `header`() = assertMatchAndNonMatch(
        Request(GET, "/").header("header", "bob"),
        { shouldHaveHeader("header", "bob") },
        { shouldHaveHeader("header", "bill") })

    @Test
    fun `header regex`() = assertMatchAndNonMatch(
        Request(GET, "/").header("header", "bob"),
        { shouldHaveHeader("header", Regex(".*bob")) },
        { shouldHaveHeader("header", Regex(".*bill")) })

    @Test
    fun headers() = assertMatchAndNonMatch(
        Request(GET, "/").header("header", "bob").header("header", "bob2"),
        { shouldHaveHeader("header", listOf("bob", "bob2")) },
        { shouldHaveHeader("header", listOf("bill")) })

    @Test
    fun `header no value`() = assertMatchAndNonMatch(
        Request(GET, "/").header("header", "bob").header("header", "bob2"),
        { shouldHaveHeader("header") },
        { shouldNotHaveHeader("header") })

    @Test
    fun `header - non-nullable string matcher`() = assertMatchAndNonMatch(
        Request(GET, "/").header("header", "bob").header("header", "bob2"),
        { shouldHaveHeader("header", contain("bob")) },
        { shouldHaveHeader("header", be<String>("bill")) })

    @Test
    fun `header lens`() =
        Header.required("bob").let {
            assertMatchAndNonMatch(
                Request(GET, "/").with(it of "bob"),
                { shouldHaveHeader(it, be("bob")) },
                { shouldHaveHeader(it, be("bill")) })
        }

    @Test
    fun `content type`() = assertMatchAndNonMatch(
        Request(GET, "/").header("Content-Type", "application/json; charset=utf-8"),
        { shouldHaveContentType(APPLICATION_JSON) },
        { shouldNotHaveContentType(APPLICATION_JSON) }
    )

    @Test
    fun `body string`() = assertMatchAndNonMatch(
        Request(GET, "/").body("bob"),
        { shouldHaveBody("bob") },
        { shouldNotHaveBody("bob") }
    )

    @Test
    fun `body regex`() =
        assertMatchAndNonMatch(
            Request(GET, "/").body("bob"),
            { shouldHaveBody(Regex(".*bob")) },
            { shouldHaveBody(Regex(".*bill")) }
        )

    @Test
    fun `body string matcher`() {
        val be: Matcher<Body> = be(Body("bob"))
        val be1 = be(Body("bill"))
        assertMatchAndNonMatch(
            Request(GET, "/").body("bob"),
            { shouldHaveBody(be) },
            { shouldHaveBody(be1) }
        )
    }

    @Test
    fun `body non-nullable string matcher`() =
        assertMatchAndNonMatch(
            Request(GET, "/").body("bob"),
            { shouldHaveBody(contain("bo")) },
            { shouldHaveBody(contain("foo")) }
        )

    @Test
    fun `body matcher`() =
        assertMatchAndNonMatch(
            Request(GET, "/").body("bob"),
            { shouldHaveBody(be<String>("bob")) },
            { shouldHaveBody(be<String>("bill")) }
        )

    @Test
    fun `json body matcher`() = assertMatchAndNonMatch(
        Request(GET, "/").body("""{"hello":"world"}"""),
        Jackson.haveBody("""{"hello":"world"}"""),
        Jackson.haveBody("""{"hello":"w2orld"}""")
    )

    @Test
    fun `json node body matcher`() = assertMatchAndNonMatch(
        Request(GET, "/").body("""{"hello":"world"}"""),
        Jackson.haveBody(be(Jackson.obj("hello" to Jackson.string("world")))),
        Jackson.haveBody(be(Jackson.obj("hello" to Jackson.string("wo2rld"))))
    )

    @Test
    fun `json node body equal matcher`() = assertMatchAndNonMatch(
        Request(GET, "/").body("""{"hello":"world"}"""),
        Jackson.haveBody(Jackson.obj("hello" to Jackson.string("world"))),
        Jackson.haveBody(Jackson.obj("hello" to Jackson.string("wo2rld")))
    )

    @Test
    fun `json node body equal matcher - with numbers`() = assertMatchAndNonMatch(
        Request(GET, "/").body("""{"hello":2}"""),
        Jackson.haveBody(Jackson.obj("hello" to Jackson.number(2))),
        Jackson.haveBody(Jackson.obj("hello" to Jackson.number(42)))
    )

    @Test
    fun `json node body equal matcher - with longs`() = assertMatchAndNonMatch(
        Request(GET, "/").body("""{"hello":2}"""),
        Jackson.haveBody(Jackson.obj("hello" to Jackson.number(2L))),
        Jackson.haveBody(Jackson.obj("hello" to Jackson.number(42L)))
    )

    @Test
    fun `body lens`() =
        Body.string(TEXT_PLAIN).toLens().let {
            assertMatchAndNonMatch(
                Request(GET, "/").with(it of "bob"),
                { shouldHaveBody(it, be("bob")) },
                { shouldHaveBody(it, be("bill")) }
            )
        }
}
