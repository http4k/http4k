package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import org.http4k.core.Body
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.ContentType.Companion.TEXT_XML
import org.http4k.core.Credentials
import org.http4k.core.MemoryBody
import org.http4k.core.MemoryRequest
import org.http4k.core.MemoryResponse
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FOUND
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.MOVED_PERMANENTLY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.core.parse
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.GzipCompressionMode.Memory
import org.http4k.filter.GzipCompressionMode.Mixed
import org.http4k.filter.GzipCompressionMode.Streaming
import org.http4k.filter.SamplingDecision.Companion.SAMPLE
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.bind
import org.http4k.routing.reverseProxy
import org.http4k.routing.routes
import org.http4k.security.CredentialsProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.Deflater.BEST_SPEED

class ClientFiltersTest {
    val server = { request: Request ->
        when (request.uri.path) {
            "/redirect" -> Response(FOUND).header("location", "/ok")
            "/see-other" -> Response(SEE_OTHER).header("location", "/ok-with-no-body")
            "/loop" -> Response(FOUND).header("location", "/loop")
            "/absolute-target" -> if (request.uri.host == "example.com") Response(OK).body("absolute") else Response(INTERNAL_SERVER_ERROR)
            "/absolute-redirect" -> Response(MOVED_PERMANENTLY).header("location", "http://example.com/absolute-target")
            "/redirect-with-charset" -> Response(MOVED_PERMANENTLY).header("location", "/destination; charset=utf8")
            "/destination" -> Response(OK).body("destination")
            "/ok" -> Response(OK).body("ok")
            "/ok-with-no-body" -> Response(OK).body(request.body)
            else -> Response(OK).let { if (request.query("foo") != null) it.body("with query") else it }
        }
    }

    private val followRedirects = ClientFilters.FollowRedirects().then(server)

    @Test
    fun `see other redirect doesn't forward any payload`() {
        val response = followRedirects(Request(GET, "http://myhost/see-other").body("body here"))
        assertThat(response.status, equalTo(OK))
        assertThat(response.body, equalTo(EMPTY))
    }

    @Test
    fun `does not follow redirect by default`() {
        val defaultClient = server
        assertThat(
            defaultClient(Request(GET, "http://myhost/redirect")),
            equalTo(Response(FOUND).header("location", "/ok"))
        )
    }

    @Test
    fun `follow redirects does not route to the wrong host`() {
        val app = ClientFilters.FollowRedirects()
            .then(reverseProxy(
                "host1" to { Response(FOUND).with(Header.LOCATION of Uri.of("http://host2")) },
                "host2" to { Response(OK).body("hello") }
            ))

        assertThat(app(Request(GET, "http://host1").header("host", "host1")), hasBody("hello"))
    }

    @Test
    fun `follow redirects sets the original host on local redirect`() {
        val app = ClientFilters.FollowRedirects()
            .then(routes(
                "/" bind GET to { Response(FOUND).header("location", "/ok") },
                "/ok" bind GET to { req: Request -> Response(OK).body(req.uri.toString()) }
            ))

        assertThat(app(Request(GET, "http://host/")), hasBody("http://host/ok"))
        assertThat(app(Request(GET, "http://host/").header("host", "host2")), hasBody("http://host/ok"))
    }

    @Test
    fun `follows redirect for temporary redirect response`() {
        assertThat(followRedirects(Request(GET, "http://myhost/redirect")), equalTo(Response(OK).body("ok")))
    }

    @Test
    fun `follows redirect for post`() {
        assertThat(followRedirects(Request(POST, "http://myhost/redirect")), equalTo(Response(OK).body("ok")))
    }

    @Test
    fun `follows redirect for put`() {
        assertThat(followRedirects(Request(PUT, "http://myhost/redirect")), equalTo(Response(OK).body("ok")))
    }

    @Test
    fun `follow redirects in-memory routed handler`() {
        val server = routes(
            "/ok" bind GET to { Response(OK) },
            "/redirect" bind GET to { Response(SEE_OTHER).header("Location", "/ok") }
        )
        val client = ClientFilters.FollowRedirects().then(server)
        assertThat(client(Request(GET, "http://myhost/ok")).status, equalTo(OK))
        assertThat(client(Request(GET, "http://myhost/redirect")).status, equalTo(OK))
    }

    @Test
    fun `supports absolute redirects`() {
        assertThat(
            followRedirects(Request(GET, "http://myhost/absolute-redirect")),
            equalTo(Response(OK).body("absolute"))
        )
    }

    @Test
    fun `discards query parameters in relative redirects`() {
        assertThat(followRedirects(Request(GET, "http://myhost/redirect?foo=bar")), equalTo(Response(OK).body("ok")))
    }

    @Test
    fun `discards charset from location header`() {
        assertThat(
            followRedirects(Request(GET, "http://myhost/redirect-with-charset")),
            equalTo(Response(OK).body("destination"))
        )
    }

    @Test
    fun `prevents redirection loop after 10 redirects`() {
        try {
            followRedirects(Request(GET, "http://myhost/loop"))
            fail("should have looped")
        } catch (e: IllegalStateException) {
            assertThat(e.message, equalTo("Too many redirection"))
        }
    }

    @BeforeEach
    fun before() {
        ZipkinTracesStorage.INTERNAL_THREAD_LOCAL.remove()
    }

    @Test
    fun `adds request tracing to outgoing request when already present`() {
        val zipkinTraces =
            ZipkinTraces(TraceId("originalTraceId"), TraceId("originalSpanId"), TraceId("originalParentId"), SAMPLE)
        ZipkinTracesStorage.THREAD_LOCAL.setForCurrentThread(zipkinTraces)

        var start: Pair<Request, ZipkinTraces>? = null
        var end: Triple<Request, Response, ZipkinTraces>? = null

        val svc = ClientFilters.RequestTracing(
            { req, trace -> start = req to trace },
            { req, resp, trace -> end = Triple(req, resp, trace) }
        ).then {
            val actual = ZipkinTraces(it)
            assertThat(
                actual,
                equalTo(ZipkinTraces(TraceId("originalTraceId"), actual.spanId, TraceId("originalSpanId"), SAMPLE))
            )
            assertThat(actual.spanId, !equalTo(zipkinTraces.spanId))
            Response(OK)
        }

        assertThat(svc(Request(GET, "")), equalTo(Response(OK)))
        assertThat(
            start,
            equalTo(
                Request(GET, "") to ZipkinTraces(
                    TraceId("originalTraceId"),
                    end!!.third.spanId,
                    TraceId("originalSpanId"),
                    SAMPLE
                )
            )
        )
        assertThat(
            end,
            equalTo(
                Triple(
                    Request(GET, ""),
                    Response(OK),
                    ZipkinTraces(TraceId("originalTraceId"), end!!.third.spanId, TraceId("originalSpanId"), SAMPLE)
                )
            )
        )
    }

    @Test
    fun `adds new request tracing to outgoing request when not present`() {
        val svc = ClientFilters.RequestTracing().then { it ->
            val actual = ZipkinTraces(it)
            assertThat(actual, present())
            assertThat(actual.parentSpanId, present())
            Response(OK)
        }

        assertThat(svc(Request(GET, "")), equalTo(Response(OK)))
    }

    @Test
    fun `set host on client`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost:123"))
            .then { Response(OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(
            handler(Request(GET, "/loop")),
            hasBody("http://localhost:123/loop").and(hasHeader("Host", "localhost:123"))
        )
    }

    @Test
    fun `set content type on client`() {
        val handler = ClientFilters.SetContentType(TEXT_XML).then { Response(OK).headers(it.headers) }
        assertThat(handler(Request(GET, "/")), hasContentType(TEXT_XML))
    }

    @Test
    fun `set host without port on client`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost"))
            .then { Response(OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(handler(Request(GET, "/loop")), hasBody("http://localhost/loop").and(hasHeader("Host", "localhost")))
    }

    @Test
    fun `set host without port on client does not set path`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost/a-path"))
            .then { Response(OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(handler(Request(GET, "/loop")), hasBody("http://localhost/loop").and(hasHeader("Host", "localhost")))
    }

    @Test
    fun `set base uri appends path`() {
        val handler = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost/a-path"))
            .then { Response(OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(
            handler(Request(GET, "/loop")),
            hasBody("http://localhost/a-path/loop").and(hasHeader("Host", "localhost"))
        )
    }

    @Test
    fun `set x-forwarded-host`() {
        val handler = ClientFilters.SetXForwardedHost().then {
            Response(OK)
                .header("Host", it.header("Host"))
                .header("X-forwarded-host", it.header("X-forwarded-host"))
                .body(it.uri.toString())
        }
        assertThat(
            handler(Request(GET, "/").header("Host", "somehost")),
            hasHeader("Host", "somehost").and(hasHeader("X-forwarded-host", "somehost"))
        )
    }

    @Test
    fun `set base uri appends path and copy other uri details`() {
        val handler = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost/a-path?a=b"))
            .then { Response(OK).header("Host", it.header("Host")).body(it.toString()) }

        val response = handler(Request(GET, "/loop").query("foo", "bar"))

        val reconstructedRequest = Request.parse(response.bodyString())
        assertThat(
            reconstructedRequest,
            equalTo(
                Request(GET, "http://localhost/a-path/loop").query("a", "b").query("foo", "bar")
                    .header("Host", "localhost")
            )
        )
    }

    @Nested
    inner class Gzip {
        @Test
        fun `requests have an accept-encoding encoding with gzip`() {
            val handler = ClientFilters.GZip().then {
                assertThat(it, hasHeader("accept-encoding", "gzip"))
                Response(OK)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK))
        }

        @Test
        fun `gzip request and gunzip in-memory response`() {
            val handler = ClientFilters.GZip().then {
                assertThat(
                    it,
                    hasHeader("content-encoding", "gzip").and(hasBody(equalTo<Body>(Body("hello").gzipped().body)))
                )
                Response(OK).header("content-encoding", "gzip").body(it.body)
            }

            assertThat(handler(Request(GET, "/").body("hello")), hasBody("hello"))
        }

        @Test
        fun `in-memory empty bodies are not encoded`() {
            val handler = ClientFilters.GZip().then {
                assertThat(it, hasBody(equalTo<Body>(EMPTY)).and(!hasHeader("content-encoding", "gzip")))
                Response(OK).body(EMPTY)
            }

            assertThat(handler(Request(GET, "/").body(EMPTY)), hasStatus(OK))
        }

        @Test
        fun `in-memory encoded empty responses are handled`() {
            val handler = ClientFilters.GZip().then {
                Response(OK).header("content-encoding", "gzip").body(EMPTY)
            }

            assertThat(handler(Request(GET, "/").body(EMPTY)), hasStatus(OK))
        }

        @Test
        fun `gzip request and gunzip streamed response`() {
            val handler = ClientFilters.GZip(Streaming()).then {
                assertThat(
                    it,
                    hasHeader(
                        "content-encoding",
                        "gzip"
                    ).and(hasBody(equalTo<Body>(Body("hello").gzippedStream().body)))
                )
                Response(OK).header("content-encoding", "gzip").body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(GET, "/").body("hello")), hasStatus(OK))
        }

        @Test
        fun `streaming empty bodies are not encoded`() {
            val handler = ClientFilters.GZip(Streaming()).then {
                assertThat(it, hasBody(equalTo<Body>(EMPTY)).and(!hasHeader("content-encoding", "gzip")))
                Response(OK).body(EMPTY)
            }

            assertThat(handler(Request(GET, "/").body(EMPTY)), hasStatus(OK))
        }

        @Test
        fun `streaming encoded empty responses are handled`() {
            val handler = ClientFilters.GZip(Streaming()).then {
                Response(OK).header("content-encoding", "gzip").body(EMPTY)
            }

            assertThat(handler(Request(GET, "/").body(EMPTY)), hasStatus(OK))
        }

        @Test
        fun `gzip stream request in mixed should return gzip stream body`() {
            val handler = ClientFilters.GZip(Mixed()).then {
                assertThat(
                    it,
                    hasHeader(
                        "content-encoding",
                        "gzip"
                    ).and(hasBody(equalTo<Body>(Body("hello").gzippedStream().body)))
                )
                Response(OK).header("content-encoding", "gzip").body(Body("world").gzippedStream().body)
            }

            assertThat(handler(Request(GET, "/").body("hello".byteInputStream())), hasStatus(OK))
        }

        @Test
        fun `gzip memory request in mixed should return gzip memory body`() {
            val handler = ClientFilters.GZip(Mixed()).then {
                assertThat(
                    it,
                    hasHeader(
                        "content-encoding",
                        "gzip"
                    ).and(hasBody(equalTo<Body>(Body("hello").gzipped().body)))
                )
                Response(OK).header("content-encoding", "gzip").body(Body("world").gzipped().body)
            }

            assertThat(handler(Request(GET, "/").body("hello")), hasStatus(OK))
        }

        @Test
        fun `passes through non-gzipped response`() {
            val handler = ClientFilters.GZip().then {
                Response(OK).body("hello")
            }

            assertThat(handler(Request(GET, "/").body("hello")), hasBody("hello"))
        }
    }

    @Nested
    inner class AcceptGZip {
        @Test
        fun `request bodies are not encoded`() {
            val handler = ClientFilters.AcceptGZip().then {
                assertThat(
                    it, hasBody(equalTo<String>("a value"))
                        .and(!hasHeader("content-encoding", "gzip"))
                )
                Response(OK)
            }

            assertThat(handler(Request(GET, "/").body("a value")), hasStatus(OK))
        }

        @Test
        fun `requests have an accept-encoding encoding with gzip`() {
            val handler = ClientFilters.AcceptGZip().then {
                assertThat(it, hasHeader("accept-encoding", "gzip"))
                Response(OK)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK))
        }

        @Test
        fun `in-memory encoded empty responses are handled`() {
            val handler = ClientFilters.AcceptGZip().then {
                Response(OK).header("content-encoding", "gzip").body(EMPTY)
            }

            assertThat(handler(Request(GET, "/").body(EMPTY)), hasStatus(OK).and(hasBody("")))
        }

        @Test
        fun `streaming encoded empty responses are handled`() {
            val handler = ClientFilters.AcceptGZip(Streaming()).then {
                Response(OK).header("content-encoding", "gzip").body(EMPTY)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK).and(hasBody("")))
        }

        @Test
        fun `in-memory responses are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(Memory()).then {
                Response(OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK).and(hasBody("hello")))
        }

        @Test
        fun `in-memory responses with compression level are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(Memory(BEST_SPEED)).then {
                Response(OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK).and(hasBody("hello")))
        }

        @Test
        fun `streaming responses are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(Streaming()).then {
                Response(OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK).and(hasBody("hello")))
        }

        @Test
        fun `streaming responses with compression level are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(Streaming(BEST_SPEED)).then {
                Response(OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(GET, "/")), hasStatus(OK).and(hasBody("hello")))
        }

        @Test
        fun `passes through non-gzipped response`() {
            val handler = ClientFilters.AcceptGZip().then {
                Response(OK).body("hello")
            }

            assertThat(handler(Request(GET, "/")), hasBody("hello"))
        }
    }

    @Test
    fun `clean proxy cleans request and response by reconstructing it on the way in and out`() {

        val captured = AtomicReference<Request>()

        val req = Request(GET, "")

        val resp = Response(OK)

        val app = ClientFilters.CleanProxy().then { r: Request ->
            captured.set(r)
            RoutedResponse(resp, UriTemplate.from("foo"))
        }

        val output = app(RoutedRequest(req, UriTemplate.from("foo")))

        assertThat(captured.get(), equalTo(req).and(isA<MemoryRequest>()))
        assertThat(output, equalTo(resp).and(isA<MemoryResponse>()))
    }

    @Test
    fun `can do proxy basic auth`() {
        val captured = AtomicReference<Request>()
        val handler = ClientFilters.ProxyBasicAuth(CredentialsProvider { Credentials("bob", "password") }).then { req ->
            captured.set(req)
            Response(OK).body("hello")
        }
        handler(Request(GET, "/"))
        assertThat(captured.get(), hasHeader("Proxy-Authorization", "Basic Ym9iOnBhc3N3b3Jk"))
    }

    @Test
    fun `set x-forwarded-host header from the host header`() {
        val handler = ClientFilters.SetXForwardedHost().then {
            assertThat(it, hasHeader("x-forwarded-host", "bobhost").and(hasHeader("host", "bobhost")))
            Response(OK)
        }
        handler(Request(GET, "/").header("host", "bobhost"))
    }
}
