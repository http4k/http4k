package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.matches
import org.junit.jupiter.api.Test

class RouterMatchingTest {

    @Test
    fun `lens router`() {
        val router = Query.int().required("foo").matches { it > 5 }
        assertThat(router(Request(GET, "").query("foo", "6")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").query("foo", "5")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").query("foo", "bar")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `headers router`() {
        val router = headers("foo", "bar")
        assertThat(router(Request(GET, "").header("foo", "1").header("bar", "2")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").header("foo", "1")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").header("bar", "2")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").header("foo2", "5")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `header predicate router`() {
        val router = header("foo") { it == "bar" }
        assertThat(router(Request(GET, "").header("foo", "1").header("foo", "bar")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").header("foo", "1")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `header value router`() {
        val router = header("foo", "bar")
        assertThat(router(Request(GET, "").header("foo", "1").header("foo", "bar")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").header("foo", "1")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `query predicate router`() {
        val router = query("foo") { it == "bar" }
        assertThat(router(Request(GET, "").query("foo", "1").query("foo", "bar")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").query("foo", "1")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `query value router`() {
        val router = query("foo", "bar")
        assertThat(router(Request(GET, "").query("foo", "1").query("foo", "bar")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").query("foo", "1")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `queries router`() {
        val router = queries("foo", "bar")
        assertThat(router(Request(GET, "").query("foo", "1").query("bar", "2")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(GET, "").query("foo", "1")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").query("bar", "2")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").query("foo2", "5")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `generic router`() {
        val router = Predicate("foo", Status.NOT_FOUND, { r: Request -> r.method == GET })
        assertThat(router(Request(GET, "")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(POST, "")), equalTo(PredicateResult.NotMatched()))
        assertThat(router.description, equalTo("foo"))
    }

    @Test
    fun `method router`() {
        val router = GET.asPredicate()
        assertThat(router(Request(GET, "")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(POST, "")), equalTo(PredicateResult.NotMatched(METHOD_NOT_ALLOWED)))
    }

    @Test
    fun `fallback router`() {
        assertThat(Fallback(Request(GET, "")), equalTo(PredicateResult.Matched))
        assertThat(Fallback(Request(POST, "")), equalTo(PredicateResult.Matched))
    }

    @Test
    fun `body string router`() {
        val router = body { it: String -> it == "hello" }
        assertThat(router(Request(POST, "")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(POST, "").body("anything")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(POST, "").body("hello")), equalTo(PredicateResult.Matched))
    }

    @Test
    fun `body router`() {
        val router = body { it: Body -> it.length?.toInt() == 3 }
        assertThat(router(Request(POST, "")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(POST, "").body("anything")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(POST, "").body("foo")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(POST, "").body("bar")), equalTo(PredicateResult.Matched))
    }

    @Test
    fun `composite router`() {
        val router = GET.and(header("foo", "bar").and(query("bar", "foo")))
        assertThat(router(Request(GET, "").header("foo", "bar").query("bar", "foo")), equalTo(PredicateResult.Matched))
        assertThat(router(Request(POST, "").header("foo", "bar").query("bar", "foo")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").header("foo", "bar")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").query("bar", "foo")), equalTo(PredicateResult.NotMatched()))
    }

    @Test
    fun `composite router with a bound router`() {
        val router = GET.asPredicate().and(header("foo", "bar"))

        assertThat(router(Request(GET, "")), equalTo(PredicateResult.NotMatched()))
        assertThat(router(Request(GET, "").header("foo", "bar")), equalTo(PredicateResult.Matched))
    }
}
