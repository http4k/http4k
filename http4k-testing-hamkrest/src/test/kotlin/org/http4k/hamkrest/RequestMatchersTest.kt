package org.http4k.hamkrest

import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
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
    fun `method`() = assertMatchAndNonMatch(Request(GET, "/bob"), hasMethod(GET), hasMethod(POST))

    @Test
    fun `uri as string`() = assertMatchAndNonMatch(Request(GET, "/bob"), hasUri("/bob"), hasUri("/bill"))

    @Test
    fun `uri as uri`() = assertMatchAndNonMatch(Request(GET, "/bob"), hasUri(Uri.of("/bob")), hasUri(Uri.of("/bill")))

    @Test
    fun `uri as regex`() = assertMatchAndNonMatch(Request(GET, "/bob123"), hasUri(Regex(".*123")), hasUri(Regex(".*124")))

    @Test
    fun `form`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"), hasForm("form", "bob"), hasForm("form", "bill"))

    @Test
    fun `form as matcher`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"), hasForm("form", containsSubstring("bob")), hasForm("form", equalTo("bill")))

    @Test
    fun `form as regex`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"), hasForm("form", Regex(".*bob")), hasForm("form", Regex(".*bill")))

    @Test
    fun `query`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"), hasQuery("form", "bob"), hasQuery("form", "bill"))

    @Test
    fun `query - matcher`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"), hasQuery("form", equalTo("bob")), hasQuery("form", containsSubstring("bill")))

    @Test
    fun `query as regex`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"), hasQuery("form", Regex(".*bob")), hasQuery("form", Regex(".*bill")))

    @Test
    fun `queries`() = assertMatchAndNonMatch(Request(GET, "/bob?query=bob&query=bob2"), hasQuery("query", listOf("bob", "bob2")), hasQuery("query", listOf("bill")))

    @Test
    fun `query lens`() =
        Query.required("bob").let {
            assertMatchAndNonMatch(Request(GET, "/").with(it of "bob"), hasQuery(it, equalTo("bob")), hasQuery(it, equalTo("bill")))
        }

    @Test
    fun `cookie`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), hasCookie(Cookie("name", "bob")), hasCookie(Cookie("name", "bill")))

    @Test
    fun `cookie matcher`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), hasCookie("name", equalTo(Cookie("name", "bob"))), hasCookie("name", equalTo(Cookie("name", "bill"))))

    @Test
    fun `cookie as a regex`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), hasCookie("name", Regex(".*bob")), hasCookie("name", Regex(".*bill")))

    @Test
    fun `cookie value`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")), hasCookie("name", "bob"), hasCookie("name", "bill"))
}
