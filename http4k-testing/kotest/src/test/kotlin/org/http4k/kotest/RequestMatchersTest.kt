package org.http4k.kotest

import io.kotest.matchers.be
import io.kotest.matchers.string.contain
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.with
import org.http4k.lens.Query
import org.junit.jupiter.api.Test

class RequestMatchersTest {

    @Test
    fun method() = assertMatchAndNonMatch(Request(GET, "/bob"), haveMethod(GET), haveMethod(POST))

    @Test
    fun `uri as string`() = assertMatchAndNonMatch(Request(GET, "/bob"), haveUri("/bob"), haveUri("/bill"))

    @Test
    fun `uri as uri`() = assertMatchAndNonMatch(Request(GET, "/bob"), haveUri(Uri.of("/bob")), haveUri(Uri.of("/bill")))

    @Test
    fun `uri as regex`() = assertMatchAndNonMatch(Request(GET, "/bob123"), haveUri(Regex(".*123")), haveUri(Regex(".*124")))

    @Test
    fun form() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"), haveForm("form", "bob"), haveForm("form", "bill"))

    @Test
    fun `form as matcher`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"), haveForm("form", contain("bob")), haveForm("form", be<String>("bill")))

    @Test
    fun `form as regex`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"), haveForm("form", Regex(".*bob")), haveForm("form", Regex(".*bill")))

    @Test
    fun query() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"), haveQuery("form", "bob"), haveQuery("form", "bill"))

    @Test
    fun `query - matcher`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"), haveQuery("form", be<String>("bob")), haveQuery("form", contain("bill")))

    @Test
    fun `query as regex`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"), haveQuery("form", Regex(".*bob")), haveQuery("form", Regex(".*bill")))

    @Test
    fun queries() = assertMatchAndNonMatch(Request(GET, "/bob?query=bob&query=bob2"), haveQuery("query", listOf("bob", "bob2")), haveQuery("query", listOf("bill")))

    @Test
    fun `query lens`() =
        Query.required("bob").let {
            assertMatchAndNonMatch(Request(GET, "/").with(it of "bob"), haveQuery(it, be("bob")), haveQuery(it, be("bill")))
        }

    @Test
    fun cookie() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), haveCookie(Cookie("name", "bob")), haveCookie(Cookie("name", "bill")))

    @Test
    fun `cookie matcher`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), haveCookie("name", be(Cookie("name", "bob"))), haveCookie("name", be(Cookie("name", "bill"))))

    @Test
    fun `cookie as a regex`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), haveCookie("name", Regex(".*bob")), haveCookie("name", Regex(".*bill")))

    @Test
    fun `cookie value`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), haveCookie("name", "bob"), haveCookie("name", "bill"))
}
