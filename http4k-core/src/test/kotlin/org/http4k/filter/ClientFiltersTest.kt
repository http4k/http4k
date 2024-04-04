package org.http4k.filter

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Credentials
import org.http4k.core.MemoryRequest
import org.http4k.core.MemoryResponse
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.core.parse
import org.http4k.core.then
import org.http4k.core.with
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
import java.util.zip.Deflater

class ClientFiltersTest {
    val server = { request: Request ->
        when (request.uri.path) {
            "/redirect" -> Response(Status.FOUND).header("location", "/ok")
            "/see-other" -> Response(Status.SEE_OTHER).header("location", "/ok-with-no-body")
            "/loop" -> Response(Status.FOUND).header("location", "/loop")
            "/absolute-target" -> if (request.uri.host == "example.com") Response(Status.OK).body("absolute") else Response(
                Status.INTERNAL_SERVER_ERROR
            )

            "/absolute-redirect" -> Response(Status.MOVED_PERMANENTLY).header(
                "location",
                "http://example.com/absolute-target"
            )

            "/redirect-with-charset" -> Response(Status.MOVED_PERMANENTLY).header(
                "location",
                "/destination; charset=utf8"
            )

            "/destination" -> Response(Status.OK).body("destination")
            "/ok" -> Response(Status.OK).body("ok")
            "/ok-with-no-body" -> Response(Status.OK).body(request.body)
            else -> Response(Status.OK).let { if (request.query("foo") != null) it.body("with query") else it }
        }
    }

    private val followRedirects = ClientFilters.FollowRedirects().then(server)

    @Test
    fun `see other redirect doesn't forward any payload`() {
        val response = followRedirects(Request(Method.GET, "http://myhost/see-other").body("body here"))
        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.body, equalTo(Body.EMPTY))
    }

    @Test
    fun `does not follow redirect by default`() {
        val defaultClient = server
        assertThat(
            defaultClient(Request(Method.GET, "http://myhost/redirect")),
            equalTo(Response(Status.FOUND).header("location", "/ok"))
        )
    }

    @Test
    fun `follow redirects does not route to the wrong host`() {
        val app = ClientFilters.FollowRedirects()
            .then(reverseProxy(
                "host1" to { Response(Status.FOUND).with(Header.LOCATION of Uri.of("http://host2")) },
                "host2" to { Response(Status.OK).body("hello") }
            ))

        assertThat(app(Request(Method.GET, "http://host1").header("host", "host1")), hasBody("hello"))
    }

    @Test
    fun `follow redirects sets the original host on local redirect`() {
        val app = ClientFilters.FollowRedirects()
            .then(routes(
                "/" bind Method.GET to { Response(Status.FOUND).header("location", "/ok") },
                "/ok" bind Method.GET to { req: Request -> Response(Status.OK).body(req.uri.toString()) }
            ))

        assertThat(app(Request(Method.GET, "http://host/")), hasBody("http://host/ok"))
        assertThat(app(Request(Method.GET, "http://host/").header("host", "host2")), hasBody("http://host/ok"))
    }

    @Test
    fun `follows redirect for temporary redirect response`() {
        assertThat(
            followRedirects(Request(Method.GET, "http://myhost/redirect")),
            equalTo(Response(Status.OK).body("ok"))
        )
    }

    @Test
    fun `follows redirect for post`() {
        assertThat(
            followRedirects(Request(Method.POST, "http://myhost/redirect")),
            equalTo(Response(Status.OK).body("ok"))
        )
    }

    @Test
    fun `follows redirect for put`() {
        assertThat(
            followRedirects(Request(Method.PUT, "http://myhost/redirect")),
            equalTo(Response(Status.OK).body("ok"))
        )
    }

    @Test
    fun `follow redirects in-memory routed handler`() {
        val server = routes(
            "/ok" bind Method.GET to { Response(Status.OK) },
            "/redirect" bind Method.GET to { Response(Status.SEE_OTHER).header("Location", "/ok") }
        )
        val client = ClientFilters.FollowRedirects().then(server)
        assertThat(client(Request(Method.GET, "http://myhost/ok")).status, equalTo(Status.OK))
        assertThat(client(Request(Method.GET, "http://myhost/redirect")).status, equalTo(Status.OK))
    }

    @Test
    fun `supports absolute redirects`() {
        assertThat(
            followRedirects(Request(Method.GET, "http://myhost/absolute-redirect")),
            equalTo(Response(Status.OK).body("absolute"))
        )
    }

    @Test
    fun `discards query parameters in relative redirects`() {
        assertThat(
            followRedirects(Request(Method.GET, "http://myhost/redirect?foo=bar")),
            equalTo(Response(Status.OK).body("ok"))
        )
    }

    @Test
    fun `discards charset from location header`() {
        assertThat(
            followRedirects(Request(Method.GET, "http://myhost/redirect-with-charset")),
            equalTo(Response(Status.OK).body("destination"))
        )
    }

    @Test
    fun `prevents redirection loop after 10 redirects`() {
        try {
            followRedirects(Request(Method.GET, "http://myhost/loop"))
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
            ZipkinTraces(
                TraceId("originalTraceId"),
                TraceId("originalSpanId"),
                TraceId("originalParentId"),
                SamplingDecision.SAMPLE
            )
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
                equalTo(
                    ZipkinTraces(
                        TraceId("originalTraceId"),
                        actual.spanId,
                        TraceId("originalSpanId"),
                        SamplingDecision.SAMPLE
                    )
                )
            )
            assertThat(actual.spanId, !equalTo(zipkinTraces.spanId))
            Response(Status.OK)
        }

        assertThat(svc(Request(Method.GET, "")), equalTo(Response(Status.OK)))
        assertThat(
            start,
            equalTo(
                Request(Method.GET, "") to ZipkinTraces(
                    TraceId("originalTraceId"),
                    end!!.third.spanId,
                    TraceId("originalSpanId"),
                    SamplingDecision.SAMPLE
                )
            )
        )
        assertThat(
            end,
            equalTo(
                Triple(
                    Request(Method.GET, ""),
                    Response(Status.OK),
                    ZipkinTraces(
                        TraceId("originalTraceId"),
                        end!!.third.spanId,
                        TraceId("originalSpanId"),
                        SamplingDecision.SAMPLE
                    )
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
            Response(Status.OK)
        }

        assertThat(svc(Request(Method.GET, "")), equalTo(Response(Status.OK)))
    }

    @Test
    fun `set host on client`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost:123"))
            .then { Response(Status.OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(
            handler(Request(Method.GET, "/loop")),
            hasBody("http://localhost:123/loop").and(hasHeader("Host", "localhost:123"))
        )
    }

    @Test
    fun `set content type on client`() {
        val handler =
            ClientFilters.SetContentType(ContentType.TEXT_XML).then { Response(Status.OK).headers(it.headers) }
        assertThat(handler(Request(Method.GET, "/")), hasContentType(ContentType.TEXT_XML))
    }

    @Test
    fun `set host without port on client`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost"))
            .then { Response(Status.OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(
            handler(Request(Method.GET, "/loop")),
            hasBody("http://localhost/loop").and(hasHeader("Host", "localhost"))
        )
    }

    @Test
    fun `set host without port on client does not set path`() {
        val handler = ClientFilters.SetHostFrom(Uri.of("http://localhost/a-path"))
            .then { Response(Status.OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(
            handler(Request(Method.GET, "/loop")),
            hasBody("http://localhost/loop").and(hasHeader("Host", "localhost"))
        )
    }

    @Test
    fun `set base uri appends path`() {
        val handler = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost/a-path"))
            .then { Response(Status.OK).header("Host", it.header("Host")).body(it.uri.toString()) }
        assertThat(
            handler(Request(Method.GET, "/loop")),
            hasBody("http://localhost/a-path/loop").and(hasHeader("Host", "localhost"))
        )
    }

    @Test
    fun `set x-forwarded-host`() {
        val handler = ClientFilters.SetXForwardedHost().then {
            Response(Status.OK)
                .header("Host", it.header("Host"))
                .header("X-forwarded-host", it.header("X-forwarded-host"))
                .body(it.uri.toString())
        }
        assertThat(
            handler(Request(Method.GET, "/").header("Host", "somehost")),
            hasHeader("Host", "somehost").and(hasHeader("X-forwarded-host", "somehost"))
        )
    }

    @Test
    fun `set base uri appends path and copy other uri details`() {
        val handler = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost/a-path?a=b"))
            .then { Response(Status.OK).header("Host", it.header("Host")).body(it.toString()) }

        val response = handler(Request(Method.GET, "/loop").query("foo", "bar"))

        val reconstructedRequest = Request.parse(response.bodyString())
        assertThat(
            reconstructedRequest,
            equalTo(
                Request(Method.GET, "http://localhost/a-path/loop").query("a", "b").query("foo", "bar")
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
                Response(Status.OK)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK))
        }

        @Test
        fun `gzip request and gunzip in-memory response`() {
            val handler = ClientFilters.GZip().then {
                assertThat(
                    it,
                    hasHeader("content-encoding", "gzip").and(hasBody(equalTo<Body>(Body("hello").gzipped().body)))
                )
                Response(Status.OK).header("content-encoding", "gzip").body(it.body)
            }

            assertThat(handler(Request(Method.GET, "/").body("hello")), hasBody("hello"))
        }

        @Test
        fun `in-memory empty bodies are not encoded`() {
            val handler = ClientFilters.GZip().then {
                assertThat(it, hasBody(equalTo<Body>(Body.EMPTY)).and(!hasHeader("content-encoding", "gzip")))
                Response(Status.OK).body(Body.EMPTY)
            }

            assertThat(handler(Request(Method.GET, "/").body(Body.EMPTY)), hasStatus(Status.OK))
        }

        @Test
        fun `in-memory encoded empty responses are handled`() {
            val handler = ClientFilters.GZip().then {
                Response(Status.OK).header("content-encoding", "gzip").body(Body.EMPTY)
            }

            assertThat(handler(Request(Method.GET, "/").body(Body.EMPTY)), hasStatus(Status.OK))
        }

        @Test
        fun `gzip request and gunzip streamed response`() {
            val handler = ClientFilters.GZip(GzipCompressionMode.Streaming()).then {
                assertThat(
                    it,
                    hasHeader(
                        "content-encoding",
                        "gzip"
                    ).and(hasBody(equalTo<Body>(Body("hello").gzippedStream().body)))
                )
                Response(Status.OK).header("content-encoding", "gzip").body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(Method.GET, "/").body("hello")), hasStatus(Status.OK))
        }

        @Test
        fun `streaming empty bodies are not encoded`() {
            val handler = ClientFilters.GZip(GzipCompressionMode.Streaming()).then {
                assertThat(it, hasBody(equalTo<Body>(Body.EMPTY)).and(!hasHeader("content-encoding", "gzip")))
                Response(Status.OK).body(Body.EMPTY)
            }

            assertThat(handler(Request(Method.GET, "/").body(Body.EMPTY)), hasStatus(Status.OK))
        }

        @Test
        fun `streaming encoded empty responses are handled`() {
            val handler = ClientFilters.GZip(GzipCompressionMode.Streaming()).then {
                Response(Status.OK).header("content-encoding", "gzip").body(Body.EMPTY)
            }

            assertThat(handler(Request(Method.GET, "/").body(Body.EMPTY)), hasStatus(Status.OK))
        }

        @Test
        fun `passes through non-gzipped response`() {
            val handler = ClientFilters.GZip().then {
                Response(Status.OK).body("hello")
            }

            assertThat(handler(Request(Method.GET, "/").body("hello")), hasBody("hello"))
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
                Response(Status.OK)
            }

            assertThat(handler(Request(Method.GET, "/").body("a value")), hasStatus(Status.OK))
        }

        @Test
        fun `requests have an accept-encoding encoding with gzip`() {
            val handler = ClientFilters.AcceptGZip().then {
                assertThat(it, hasHeader("accept-encoding", "gzip"))
                Response(Status.OK)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK))
        }

        @Test
        fun `in-memory encoded empty responses are handled`() {
            val handler = ClientFilters.AcceptGZip().then {
                Response(Status.OK).header("content-encoding", "gzip").body(Body.EMPTY)
            }

            assertThat(handler(Request(Method.GET, "/").body(Body.EMPTY)), hasStatus(Status.OK).and(hasBody("")))
        }

        @Test
        fun `streaming encoded empty responses are handled`() {
            val handler = ClientFilters.AcceptGZip(GzipCompressionMode.Streaming()).then {
                Response(Status.OK).header("content-encoding", "gzip").body(Body.EMPTY)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK).and(hasBody("")))
        }

        @Test
        fun `in-memory responses are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(GzipCompressionMode.Memory()).then {
                Response(Status.OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK).and(hasBody("hello")))
        }

        @Test
        fun `in-memory responses with compression level are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(GzipCompressionMode.Memory(Deflater.BEST_SPEED)).then {
                Response(Status.OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK).and(hasBody("hello")))
        }

        @Test
        fun `streaming responses are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(GzipCompressionMode.Streaming()).then {
                Response(Status.OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK).and(hasBody("hello")))
        }

        @Test
        fun `streaming responses with compression level are ungzipped`() {
            val handler = ClientFilters.AcceptGZip(GzipCompressionMode.Streaming(Deflater.BEST_SPEED)).then {
                Response(Status.OK).header("content-encoding", "gzip")
                    .body(Body("hello").gzippedStream().body)
            }

            assertThat(handler(Request(Method.GET, "/")), hasStatus(Status.OK).and(hasBody("hello")))
        }

        @Test
        fun `passes through non-gzipped response`() {
            val handler = ClientFilters.AcceptGZip().then {
                Response(Status.OK).body("hello")
            }

            assertThat(handler(Request(Method.GET, "/")), hasBody("hello"))
        }
    }

    @Test
    fun `clean proxy cleans request and response by reconstructing it on the way in and out`() {

        val captured = AtomicReference<Request>()

        val req = Request(Method.GET, "")

        val resp = Response(Status.OK)

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
            Response(Status.OK).body("hello")
        }
        handler(Request(Method.GET, "/"))
        assertThat(captured.get(), hasHeader("Proxy-Authorization", "Basic Ym9iOnBhc3N3b3Jk"))
    }

    @Test
    fun `set x-forwarded-host header from the host header`() {
        val handler = ClientFilters.SetXForwardedHost().then {
            assertThat(it, hasHeader("x-forwarded-host", "bobhost").and(hasHeader("host", "bobhost")))
            Response(Status.OK)
        }
        handler(Request(Method.GET, "/").header("host", "bobhost"))
    }
}
