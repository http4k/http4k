package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.lens.Query
import org.http4k.lens.accept
import org.http4k.lens.int
import org.http4k.lens.matches
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.junit.jupiter.api.Test

class RouterMatchingTest {

    @Test
    fun `lens router`() = runBlocking {
        val router = Query.int().required("foo").matches { it > 5 }
        assertThat(router(Request(GET, "").query("foo", "6")), isA<Matched>())
        assertThat(router(Request(GET, "").query("foo", "5")), isA<NotMatched>())
        assertThat(router(Request(GET, "").query("foo", "bar")), isA<NotMatched>())
    }

    @Test
    fun `path router`() = runBlocking {
        val router = "/bob" and GET.asRouter()
        assertThat(router(Request(GET, "bob")), isA<Matched>())
        assertThat(router(Request(GET, "/bob")), isA<Matched>())
        assertThat(router(Request(POST, "/bob")), isA<NotMatched>())
    }

    @Test
    fun `accepting content type with a path`() = runBlocking {
        val router = "bob".accepting(APPLICATION_JSON)
        assertThat(router(Request(GET, "bob")), isA<NotMatched>())
        assertThat(router(Request(GET, "/bob").accept(TEXT_EVENT_STREAM)), isA<NotMatched>())
        assertThat(router(Request(GET, "bob").accept(APPLICATION_JSON)), isA<Matched>())
    }

    @Test
    fun `headers router`() = runBlocking {
        val router = headers("foo", "bar")
        assertThat(router(Request(GET, "").header("foo", "1").header("bar", "2")), isA<Matched>())
        assertThat(router(Request(GET, "").header("foo", "1")), isA<NotMatched>())
        assertThat(router(Request(GET, "").header("bar", "2")), isA<NotMatched>())
        assertThat(router(Request(GET, "").header("foo2", "5")), isA<NotMatched>())
    }

    @Test
    fun `header predicate router`() = runBlocking {
        val router = header("foo") { it == "bar" }
        assertThat(router(Request(GET, "").header("foo", "1").header("foo", "bar")), isA<Matched>())
        assertThat(router(Request(GET, "").header("foo", "1")), isA<NotMatched>())
        assertThat(router(Request(GET, "")), isA<NotMatched>())
    }

    @Test
    fun `header value router`() = runBlocking {
        val router = header("foo", "bar")
        assertThat(router(Request(GET, "").header("foo", "1").header("foo", "bar")), isA<Matched>())
        assertThat(router(Request(GET, "").header("foo", "1")), isA<NotMatched>())
        assertThat(router(Request(GET, "")), isA<NotMatched>())
    }

    @Test
    fun `query predicate router`() = runBlocking {
        val router = query("foo") { it == "bar" }
        assertThat(router(Request(GET, "").query("foo", "1").query("foo", "bar")), isA<Matched>())
        assertThat(router(Request(GET, "").query("foo", "1")), isA<NotMatched>())
        assertThat(router(Request(GET, "")), isA<NotMatched>())
    }

    @Test
    fun `query value router`() = runBlocking {
        val router = query("foo", "bar")
        assertThat(router(Request(GET, "").query("foo", "1").query("foo", "bar")), isA<Matched>())
        assertThat(router(Request(GET, "").query("foo", "1")), isA<NotMatched>())
        assertThat(router(Request(GET, "")), isA<NotMatched>())
    }

    @Test
    fun `query present router`() = runBlocking {
        val router = query("foo")
        assertThat(router(Request(GET, "").query("foo", null)), isA<Matched>())
        assertThat(router(Request(GET, "")), isA<NotMatched>())
    }

    @Test
    fun `queries router`() = runBlocking {
        val router = queries("foo", "bar")
        assertThat(router(Request(GET, "").query("foo", "1").query("bar", "2")), isA<Matched>())
        assertThat(router(Request(GET, "").query("foo", "1")), isA<NotMatched>())
        assertThat(router(Request(GET, "").query("bar", "2")), isA<NotMatched>())
        assertThat(router(Request(GET, "").query("foo2", "5")), isA<NotMatched>())
    }

    @Test
    fun `queriesFrom router`() = runBlocking {
        val router = queriesFrom(Uri.of("http://localhost:8080?foo=boo&bar"))

        assertThat(router(Request(GET, "http://localhost:8080?foo=bar")), isA<NotMatched>())
        assertThat(router(Request(GET, "http://localhost:8080?foo=boo&bar")), isA<Matched>())
    }

    @Test
    fun `generic router`() = runBlocking {
        val router = Router("foo", NOT_FOUND, { r: Request -> r.method == GET })
        assertThat(router(Request(GET, "")), isA<Matched>())
        assertThat(router(Request(POST, "")), isA<NotMatched>())
        assertThat(router.description, equalTo(RouterDescription("foo")))
    }

    @Test
    fun `method router`() = runBlocking {
        val router = GET.asRouter()
        assertThat(router(Request(GET, "")), isA<Matched>())
        assertThat(
            router(Request(POST, "")),
            equalTo(NotMatched(METHOD_NOT_ALLOWED, RouterDescription("method == GET")))
        )
    }

    @Test
    fun `all router`() = runBlocking {
        assertThat(All(Request(GET, "")), isA<Matched>())
        assertThat(All(Request(POST, "")), isA<Matched>())
    }

    @Test
    fun `body string router`() = runBlocking {
        val router = body { it: String -> it == "hello" }
        assertThat(router(Request(POST, "")), isA<NotMatched>())
        assertThat(router(Request(POST, "").body("anything")), isA<NotMatched>())
        assertThat(router(Request(POST, "").body("hello")), isA<Matched>())
    }

    @Test
    fun `body router`() = runBlocking {
        val router = body { it: Body -> it.length?.toInt() == 3 }
        assertThat(router(Request(POST, "")), isA<NotMatched>())
        assertThat(router(Request(POST, "").body("anything")), isA<NotMatched>())
        assertThat(router(Request(POST, "").body("foo")), isA<Matched>())
        assertThat(router(Request(POST, "").body("bar")), isA<Matched>())
    }

    @Test
    fun `composite router`() = runBlocking {
        val router = GET.and(header("foo", "bar").and(query("bar", "foo")))
        assertThat(router(Request(GET, "").header("foo", "bar").query("bar", "foo")), isA<Matched>())
        assertThat(router(Request(POST, "").header("foo", "bar").query("bar", "foo")), isA<NotMatched>())
        assertThat(router(Request(GET, "").header("foo", "bar")), isA<NotMatched>())
        assertThat(router(Request(GET, "").query("bar", "foo")), isA<NotMatched>())
    }

    @Test
    fun `composite router with a bound router`() = runBlocking {
        val router = GET.asRouter().and(header("foo", "bar"))

        assertThat(router(Request(GET, "")), isA<NotMatched>())
        assertThat(router(Request(GET, "").header("foo", "bar")), isA<Matched>())
    }
}
