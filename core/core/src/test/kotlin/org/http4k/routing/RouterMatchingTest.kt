package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.matches
import org.http4k.routing.RouterMatch.MatchingHandler
import org.junit.jupiter.api.Test

class RouterMatchingTest {

    @Test
    fun `lens router`() {
        val router = Query.int().required("foo").matches { it > 5 }
        assertThat(router(Request(GET, "").query("foo", "6")), equalTo(true))
        assertThat(router(Request(GET, "").query("foo", "5")), equalTo(false))
        assertThat(router(Request(GET, "").query("foo", "bar")), equalTo(false))
    }

    @Test
    fun `headers router`() {
        val router = headers("foo", "bar")
        assertThat(router(Request(GET, "").header("foo", "1").header("bar", "2")), equalTo(true))
        assertThat(router(Request(GET, "").header("foo", "1")), equalTo(false))
        assertThat(router(Request(GET, "").header("bar", "2")), equalTo(false))
        assertThat(router(Request(GET, "").header("foo2", "5")), equalTo(false))
    }

    @Test
    fun `header predicate router`() {
        val router = header("foo") { it == "bar" }
        assertThat(router(Request(GET, "").header("foo", "1").header("foo", "bar")), equalTo(true))
        assertThat(router(Request(GET, "").header("foo", "1")), equalTo(false))
        assertThat(router(Request(GET, "")), equalTo(false))
    }

    @Test
    fun `header value router`() {
        val router = header("foo", "bar")
        assertThat(router(Request(GET, "").header("foo", "1").header("foo", "bar")), equalTo(true))
        assertThat(router(Request(GET, "").header("foo", "1")), equalTo(false))
        assertThat(router(Request(GET, "")), equalTo(false))
    }

    @Test
    fun `query predicate router`() {
        val router = query("foo") { it == "bar" }
        assertThat(router(Request(GET, "").query("foo", "1").query("foo", "bar")), equalTo(true))
        assertThat(router(Request(GET, "").query("foo", "1")), equalTo(false))
        assertThat(router(Request(GET, "")), equalTo(false))
    }

    @Test
    fun `query value router`() {
        val router = query("foo", "bar")
        assertThat(router(Request(GET, "").query("foo", "1").query("foo", "bar")), equalTo(true))
        assertThat(router(Request(GET, "").query("foo", "1")), equalTo(false))
        assertThat(router(Request(GET, "")), equalTo(false))
    }

    @Test
    fun `queries router`() {
        val router = queries("foo", "bar")
        assertThat(router(Request(GET, "").query("foo", "1").query("bar", "2")), equalTo(true))
        assertThat(router(Request(GET, "").query("foo", "1")), equalTo(false))
        assertThat(router(Request(GET, "").query("bar", "2")), equalTo(false))
        assertThat(router(Request(GET, "").query("foo2", "5")), equalTo(false))
    }

    @Test
    fun `generic router`() {
        val router = Predicate("foo", Status.NOT_FOUND, { r: Request -> r.method == GET })
        assertThat(router(Request(GET, "")), equalTo(true))
        assertThat(router(Request(POST, "")), equalTo(false))
        assertThat(router.description, equalTo(RouterDescription("foo")))
    }

    @Test
    fun `method router`() {
        val router = GET.asPredicate()
        assertThat(router(Request(GET, "")), equalTo(true))
        assertThat(router(Request(POST, "")), equalTo(false))
    }

    @Test
    fun `fallback router`() {
        assertThat(Fallback(Request(GET, "")), equalTo(true))
        assertThat(Fallback(Request(POST, "")), equalTo(true))
    }

    @Test
    fun `body string router`() {
        val router = body { it: String -> it == "hello" }
        assertThat(router(Request(POST, "")), equalTo(false))
        assertThat(router(Request(POST, "").body("anything")), equalTo(false))
        assertThat(router(Request(POST, "").body("hello")), equalTo(true))
    }

    @Test
    fun `body router`() {
        val router = body { it: Body -> it.length?.toInt() == 3 }
        assertThat(router(Request(POST, "")), equalTo(false))
        assertThat(router(Request(POST, "").body("anything")), equalTo(false))
        assertThat(router(Request(POST, "").body("foo")), equalTo(true))
        assertThat(router(Request(POST, "").body("bar")), equalTo(true))
    }

    @Test
    fun `composite router`() {
        val router = GET.and(header("foo", "bar").and(query("bar", "foo")))
        assertThat(router(Request(GET, "").header("foo", "bar").query("bar", "foo")), equalTo(true))
        assertThat(router(Request(POST, "").header("foo", "bar").query("bar", "foo")), equalTo(false))
        assertThat(router(Request(GET, "").header("foo", "bar")), equalTo(false))
        assertThat(router(Request(GET, "").query("bar", "foo")), equalTo(false))
    }

    @Test
    fun `composite router with a bound router`() {
        val router = GET.bind { Response(OK) }.and(header("foo", "bar"))

        assertThat(router(Request(GET, "")), equalTo(false))
        assertThat(router(Request(GET, "").header("foo", "bar")), isA<MatchingHandler>())
    }
}
