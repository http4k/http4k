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
    fun method() = assertMatchAndNonMatch(Request(GET, "/bob"),
        { shouldHaveMethod(GET) }, { shouldHaveMethod(POST) })

    @Test
    fun `uri as string`() = assertMatchAndNonMatch(Request(GET, "/bob"),
        { shouldHaveUri("/bob") }, { shouldHaveUri("/bill") })

    @Test
    fun `uri as uri`() = assertMatchAndNonMatch(Request(GET, "/bob"),
        { shouldHaveUri(Uri.of("/bob")) },
        { shouldHaveUri(Uri.of("/bill")) })

    @Test
    fun `uri as regex`() = assertMatchAndNonMatch(Request(GET, "/bob123"),
        { shouldHaveUri(Regex(".*123")) },
        { shouldHaveUri(Regex(".*124")) })

    @Test
    fun form() = assertMatchAndNonMatch(
        Request(GET, "/").form("form", "bob"),
        { shouldHaveForm("form", "bob") },
        { shouldHaveForm("form", "bill") })

    @Test
    fun `form as matcher`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"),
        { shouldHaveForm("form", contain("bob")) },
        { shouldHaveForm("form", be<String>("bill")) })

    @Test
    fun `form as regex`() = assertMatchAndNonMatch(Request(GET, "/").form("form", "bob"),
        { shouldHaveForm("form", Regex(".*bob")) },
        { shouldHaveForm("form", Regex(".*bill")) })

    @Test
    fun query() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"),
        { shouldHaveQuery("form", "bob") },
        { shouldHaveQuery("form", "bill") }
    )

    @Test
    fun `query - matcher`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"),
        { shouldHaveQuery("form", be<String>("bob")) },
        { shouldHaveQuery("form", contain("bill")) })

    @Test
    fun `query as regex`() = assertMatchAndNonMatch(Request(GET, "/bob?form=bob"),
        { shouldHaveQuery("form", Regex(".*bob")) },
        { shouldHaveQuery("form", Regex(".*bill")) })

    @Test
    fun queries() = assertMatchAndNonMatch(Request(GET, "/bob?query=bob&query=bob2"),
        { shouldHaveQuery("query", listOf("bob", "bob2")) },
        { shouldHaveQuery("query", listOf("bill")) })

    @Test
    fun `query lens`() =
        Query.required("bob").let {
            assertMatchAndNonMatch(Request(GET, "/").with(it of "bob"),
                { shouldHaveQuery(it, be("bob")) },
                { shouldHaveQuery(it, be("bill")) })
        }

    @Test
    fun cookie() = assertMatchAndNonMatch(
        Request(GET, "").cookie(Cookie("name", "bob")),
        { shouldHaveCookie(Cookie("name", "bob")) },
        { shouldHaveCookie(Cookie("name", "bill")) })

    @Test
    fun `cookie matcher`() = assertMatchAndNonMatch(
        Request(GET, "").cookie(Cookie("name", "bob")),
        { shouldHaveCookie("name", be(Cookie("name", "bob"))) },
        {
            shouldHaveCookie(
                "name",
                be(Cookie("name", "bill"))
            )
        })

    @Test
    fun `cookie as a regex`() = assertMatchAndNonMatch(
        Request(GET, "").cookie(Cookie("name", "bob")),
        { shouldHaveCookie("name", Regex(".*bob")) },
        { shouldHaveCookie("name", Regex(".*bill")) })

    @Test
    fun `cookie value`() = assertMatchAndNonMatch(Request(GET, "").cookie(Cookie("name", "bob")),
        { shouldHaveCookie("name", "bob") }, { shouldHaveCookie("name", "bill") })
}
