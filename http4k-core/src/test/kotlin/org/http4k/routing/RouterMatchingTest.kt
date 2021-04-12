package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.matches
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched
import org.junit.jupiter.api.Test

class RouterMatchingTest {

    @Test
    fun `lens router`() {
        val router: Router = Query.int().required("foo").matches { it > 5 }
        assertThat(router.match(Request(GET, "").query("foo", "6")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").query("foo", "5")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").query("foo", "bar")), isA<Unmatched>())
    }

    @Test
    fun `headers router`() {
        val router: Router = headers("foo", "bar")
        assertThat(router.match(Request(GET, "").header("foo", "1").header("bar", "2")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").header("foo", "1")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").header("bar", "2")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").header("foo2", "5")), isA<Unmatched>())
    }

    @Test
    fun `header predicate router`() {
        val router: Router = header("foo") { it == "bar" }
        assertThat(router.match(Request(GET, "").header("foo", "1").header("foo", "bar")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").header("foo", "1")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "")), isA<Unmatched>())
    }

    @Test
    fun `header value router`() {
        val router: Router = header("foo", "bar")
        assertThat(router.match(Request(GET, "").header("foo", "1").header("foo", "bar")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").header("foo", "1")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "")), isA<Unmatched>())
    }

    @Test
    fun `query predicate router`() {
        val router: Router = query("foo") { it == "bar" }
        assertThat(router.match(Request(GET, "").query("foo", "1").query("foo", "bar")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").query("foo", "1")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "")), isA<Unmatched>())
    }

    @Test
    fun `query value router`() {
        val router: Router = query("foo", "bar")
        assertThat(router.match(Request(GET, "").query("foo", "1").query("foo", "bar")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").query("foo", "1")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "")), isA<Unmatched>())
    }

    @Test
    fun `queries router`() {
        val router: Router = queries("foo", "bar")
        assertThat(router.match(Request(GET, "").query("foo", "1").query("bar", "2")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(GET, "").query("foo", "1")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").query("bar", "2")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").query("foo2", "5")), isA<Unmatched>())
    }

    @Test
    fun `generic router`() {
        val router = { r: Request -> r.method == GET }.asRouter()
        assertThat(router.match(Request(GET, "")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(POST, "")), isA<Unmatched>())
    }

    @Test
    fun `method router`() {
        val router = GET.asRouter()
        assertThat(router.match(Request(GET, "")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(POST, "")), isA<MethodNotMatched>())
    }

    @Test
    fun `fallback router`() {
        assertThat(Fallback.match(Request(GET, "")), isA<MatchedWithoutHandler>())
        assertThat(Fallback.match(Request(POST, "")), isA<MatchedWithoutHandler>())
    }

    @Test
    fun `body string router`() {
        val router = body { it: String -> it == "hello" }
        assertThat(router.match(Request(POST, "")), isA<Unmatched>())
        assertThat(router.match(Request(POST, "").body("anything")), isA<Unmatched>())
        assertThat(router.match(Request(POST, "").body("hello")), isA<MatchedWithoutHandler>())
    }

    @Test
    fun `body router`() {
        val router = body { it: Body -> it.length?.toInt() == 3 }
        assertThat(router.match(Request(POST, "")), isA<Unmatched>())
        assertThat(router.match(Request(POST, "").body("anything")), isA<Unmatched>())
        assertThat(router.match(Request(POST, "").body("foo")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(POST, "").body("bar")), isA<MatchedWithoutHandler>())
    }

    @Test
    fun `composite router`() {
        val router = GET.and(header("foo", "bar").and(query("bar", "foo")))
        assertThat(router.match(Request(GET, "").header("foo", "bar").query("bar", "foo")), isA<MatchedWithoutHandler>())
        assertThat(router.match(Request(POST, "").header("foo", "bar").query("bar", "foo")), isA<MethodNotMatched>())
        assertThat(router.match(Request(GET, "").header("foo", "bar")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").query("bar", "foo")), isA<Unmatched>())
    }

    @Test
    fun `composite router with a bound router`() {
        val router = GET.bind { Response(OK) }.and(header("foo", "bar"))

        assertThat(router.match(Request(GET, "")), isA<Unmatched>())
        assertThat(router.match(Request(GET, "").header("foo", "bar")), isA<MatchingHandler>())
    }
}
