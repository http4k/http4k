package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_MODIFIED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

class ClientCacheFiltersTest {

    private val request = Request(GET, "/value")

    @Test
    fun `fresh response is served from cache without hitting the network`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=100")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(1))

        time.advance(10)
        val second = client(request)
        assertThat(second, hasBody("hello"))
        assertThat(second, hasHeader("Age", "10"))
        assertThat(hits, equalTo(1))
    }

    @Test
    fun `stale response is revalidated and 304 returns the cached copy with updated headers`() {
        val time = TestTime()
        var hits = 0
        val received = mutableListOf<Request>()
        val server = { req: Request ->
            hits++
            received += req
            if (req.header("If-None-Match") != null) {
                Response(NOT_MODIFIED).header("Cache-Control", "public, max-age=100").header("ETag", "v2")
            } else {
                Response(OK).body("hello").header("Cache-Control", "public, max-age=5").header("ETag", "v1")
            }
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(1))

        time.advance(10)
        assertThat(client(request), hasBody("hello"))
        assertThat(received.last().header("If-None-Match"), equalTo("v1"))
        assertThat(hits, equalTo(2))

        time.advance(5)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `stale response is refreshed when the server returns a new 200`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("v$hits").header("Cache-Control", "public, max-age=5")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("v1"))
        assertThat(hits, equalTo(1))

        time.advance(4)
        assertThat(client(request), hasBody("v1"))
        assertThat(hits, equalTo(1))

        time.advance(5)
        assertThat(client(request), hasBody("v2"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `no-store responses are never cached`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "no-store")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `response with no-cache is always revalidated but reuses cached body on 304`() {
        val time = TestTime()
        var hits = 0
        val server = { req: Request ->
            hits++
            if (req.header("If-None-Match") != null) {
                Response(NOT_MODIFIED).header("ETag", "v1")
            } else {
                Response(OK).body("hello").header("Cache-Control", "no-cache").header("ETag", "v1")
            }
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(1)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `request with no-cache forces a revalidation even when the cached response is fresh`() {
        val time = TestTime()
        var hits = 0
        val server = { req: Request ->
            hits++
            if (req.header("If-None-Match") != null) {
                Response(NOT_MODIFIED).header("ETag", "v1")
            } else {
                Response(OK).body("hello").header("Cache-Control", "public, max-age=100").header("ETag", "v1")
            }
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        assertThat(
            client(request.header("Cache-Control", "no-cache")),
            hasBody("hello")
        )
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `immutable fresh responses skip revalidation on a reload with max-age=0`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=100, immutable").header("ETag", "v1")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(5)
        assertThat(client(request.header("Cache-Control", "max-age=0")), hasBody("hello"))
        assertThat(hits, equalTo(1))
    }

    @Test
    fun `only-if-cached returns 504 when there is nothing cached`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello")
        }
        val client = client(server, time)

        val response = client(request.header("Cache-Control", "only-if-cached"))
        assertThat(response, hasStatus(GATEWAY_TIMEOUT))
        assertThat(hits, equalTo(0))
    }

    @Test
    fun `only-if-cached serves a stale cached response`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=10")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(50)
        assertThat(client(request.header("Cache-Control", "only-if-cached")), hasBody("hello"))
        assertThat(hits, equalTo(1))
    }

    @Test
    fun `max-stale serves a stale response within the requested window`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=10")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(30)
        assertThat(client(request.header("Cache-Control", "max-stale=100")), hasBody("hello"))
        assertThat(hits, equalTo(1))
    }

    @Test
    fun `stale-while-revalidate serves the stale response without a network call`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=10, stale-while-revalidate=100")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(30)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(1))
    }

    @Test
    fun `stale-if-error serves the stale response when the revalidation fails`() {
        val time = TestTime()
        var hits = 0
        var first = true
        val server = { _: Request ->
            hits++
            if (first) {
                first = false
                Response(OK).body("hello").header("Cache-Control", "public, max-age=10, stale-if-error=100")
            } else {
                Response(INTERNAL_SERVER_ERROR)
            }
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(30)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `captures a fresh response via expires header`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Expires", rfc1123(time.now.plusSeconds(100)))
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(10)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(1))

        time.advance(200)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `age header accelerates staleness`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=100").header("Age", "90")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(20)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `heuristic caching from Last-Modified`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello")
                .header("Last-Modified", rfc1123(time.now.minusSeconds(100)))
                .header("Date", rfc1123(time.now))
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        time.advance(5)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(1))

        time.advance(6)
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `vary matched variant is reused and different variants are refetched`() {
        val time = TestTime()
        var hits = 0
        val server = { req: Request ->
            hits++
            when (req.header("Accept-Language")) {
                "en" -> Response(OK).body("english")
                    .header("Cache-Control", "public, max-age=100")
                    .header("Vary", "Accept-Language")

                "fr" -> Response(OK).body("français")
                    .header("Cache-Control", "public, max-age=100")
                    .header("Vary", "Accept-Language")

                else -> Response(OK)
            }
        }
        val client = client(server, time)

        assertThat(client(Request(GET, "/value").header("Accept-Language", "en")), hasBody("english"))
        assertThat(client(Request(GET, "/value").header("Accept-Language", "en")), hasBody("english"))
        assertThat(hits, equalTo(1))

        assertThat(client(Request(GET, "/value").header("Accept-Language", "fr")), hasBody("français"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `write methods invalidate cached entries for the target uri`() {
        val time = TestTime()
        var hits = 0
        val server = { req: Request ->
            hits++
            when (req.method) {
                GET -> Response(OK).body("hello").header("Cache-Control", "public, max-age=100")
                else -> Response(OK)
            }
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        assertThat(client(Request(POST, "/value")), hasStatus(OK))
        assertThat(client(request), hasBody("hello"))
        assertThat(hits, equalTo(3))
    }

    @Test
    fun `non-cacheable methods are passed through`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=100")
        }
        val client = client(server, time)

        assertThat(client(Request(POST, "/value")), hasBody("hello"))
        assertThat(client(Request(POST, "/value")), hasBody("hello"))
        assertThat(hits, equalTo(2))
    }

    @Test
    fun `a cached GET response serves a HEAD request`() {
        val time = TestTime()
        var hits = 0
        val server = { _: Request ->
            hits++
            Response(OK).body("hello").header("Cache-Control", "public, max-age=100").header("Content-Length", "5")
        }
        val client = client(server, time)

        assertThat(client(request), hasBody("hello"))
        assertThat(client(Request(HEAD, "/value")), hasBody(""))
        assertThat(hits, equalTo(1))
    }

    @Test
    fun `in-memory storage stores retrieves replaces removes and clears variants`() {
        val storage = InMemoryClientCacheStorage()
        val uri = Uri.of("/value")
        val cached = CachedResponse(uri, GET, listOf(), Response(OK).body("hello"), Instant.ofEpochMilli(0))

        storage.clear()
        assertThat(storage.retrieve(uri), equalTo(emptyList()))

        storage.store(uri, cached)
        assertThat(storage.retrieve(uri), equalTo(listOf(cached)))

        val replacement = CachedResponse(uri, GET, listOf(), Response(OK).body("goodbye"), Instant.ofEpochMilli(0))
        storage.store(uri, replacement)
        assertThat(storage.retrieve(uri), equalTo(listOf(replacement)))

        storage.remove(uri)
        assertThat(storage.retrieve(uri), equalTo(emptyList()))
    }

    @Test
    fun `in-memory storage evicts the least-recently-used entries`() {
        val storage = InMemoryClientCacheStorage(maxEntries = 2)
        val uriA = Uri.of("/a")
        val uriB = Uri.of("/b")
        val uriC = Uri.of("/c")
        val cachedA = CachedResponse(uriA, GET, listOf(), Response(OK), Instant.ofEpochMilli(0))
        val cachedB = CachedResponse(uriB, GET, listOf(), Response(OK), Instant.ofEpochMilli(0))
        val cachedC = CachedResponse(uriC, GET, listOf(), Response(OK), Instant.ofEpochMilli(0))

        storage.store(uriA, cachedA)
        storage.store(uriB, cachedB)
        storage.retrieve(uriA)
        storage.store(uriC, cachedC)

        assertThat(storage.retrieve(uriA), equalTo(listOf(cachedA)))
        assertThat(storage.retrieve(uriB), equalTo(emptyList()))
        assertThat(storage.retrieve(uriC), equalTo(listOf(cachedC)))
    }

    private fun client(server: HttpHandler, time: TestTime, storage: ClientCacheStorage = InMemoryClientCacheStorage()) =
        ClientCacheFilters(storage = storage, timeSource = time.source).then(server)

    private fun rfc1123(instant: Instant) =
        RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC))

    private class TestTime {
        var now: Instant = Instant.ofEpochMilli(0)
        val source: () -> Instant get() = { now }
        fun advance(seconds: Long) {
            now = now.plusSeconds(seconds)
        }
    }
}
