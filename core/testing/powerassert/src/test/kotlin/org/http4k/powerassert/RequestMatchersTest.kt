package org.http4k.powerassert

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
    fun method() {
        val request = Request(GET, "/bob")
        assert(request.hasMethod(GET))
        assert(!request.hasMethod(POST))
    }

    @Test
    fun `uri as string`() {
        val request = Request(GET, "/bob")
        assert(request.hasUri("/bob"))
        assert(!request.hasUri("/bill"))
    }

    @Test
    fun `uri as uri`() {
        val request = Request(GET, "/bob")
        assert(request.hasUri(Uri.of("/bob")))
        assert(!request.hasUri(Uri.of("/bill")))
    }

    @Test
    fun `uri as regex`() {
        val request = Request(GET, "/bob123")
        assert(request.hasUri(Regex(".*123")))
        assert(!request.hasUri(Regex(".*124")))
    }

    @Test
    fun form() {
        val request = Request(GET, "/").form("form", "bob")
        assert(request.hasForm("form", "bob" as String?))
        assert(!request.hasForm("form", "bill" as String?))
    }

    @Test
    fun `form as regex`() {
        val request = Request(GET, "/").form("form", "bob")
        assert(request.hasForm("form", Regex(".*bob")))
        assert(!request.hasForm("form", Regex(".*bill")))
    }

    @Test
    fun query() {
        val request = Request(GET, "/bob?form=bob")
        assert(request.hasQuery("form", "bob" as String?))
        assert(!request.hasQuery("form", "bill" as String?))
    }

    @Test
    fun `query as regex`() {
        val request = Request(GET, "/bob?form=bob")
        assert(request.hasQuery("form", Regex(".*bob")))
        assert(!request.hasQuery("form", Regex(".*bill")))
    }

    @Test
    fun queries() {
        val request = Request(GET, "/bob?query=bob&query=bob2")
        assert(request.hasQuery("query", listOf("bob", "bob2")))
        assert(!request.hasQuery("query", listOf("bill")))
    }

    @Test
    fun `query lens`() {
        val bobQuery = Query.required("bob")
        val requestWithQuery = Request(GET, "/").with(bobQuery of "bob")
        
        assert(requestWithQuery.hasQuery(bobQuery, "bob"))
        assert(!requestWithQuery.hasQuery(bobQuery, "bill"))
    }

    @Test
    fun cookie() {
        val request = Request(GET, "").cookie(Cookie("name", "bob"))
        assert(request.hasCookie(Cookie("name", "bob")))
        assert(!request.hasCookie(Cookie("name", "bill")))
    }

    @Test
    fun `cookie matcher`() {
        val request = Request(GET, "").cookie(Cookie("name", "bob"))
        assert(request.hasCookie("name", Cookie("name", "bob")))
        assert(!request.hasCookie("name", Cookie("name", "bill")))
    }

    @Test
    fun `cookie as a regex`() {
        val request = Request(GET, "").cookie(Cookie("name", "bob"))
        assert(request.hasCookie("name", Regex(".*bob")))
        assert(!request.hasCookie("name", Regex(".*bill")))
    }

    @Test
    fun `cookie value`() {
        val request = Request(GET, "").cookie(Cookie("name", "bob"))
        assert(request.hasCookie("name", "bob"))
        assert(!request.hasCookie("name", "bill"))
    }
}