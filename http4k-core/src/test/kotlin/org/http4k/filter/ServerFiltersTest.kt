package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.throws
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Filter
import org.http4k.core.Headers
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.then
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.GzipCompressionMode.Streaming
import org.http4k.filter.SamplingDecision.Companion.DO_NOT_SAMPLE
import org.http4k.filter.SamplingDecision.Companion.SAMPLE
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Header
import org.http4k.lens.Invalid
import org.http4k.lens.LensFailure
import org.http4k.lens.Missing
import org.http4k.lens.Unsupported
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

class ServerFiltersTest {

    @BeforeEach
    fun before() {
        ZipkinTraces.THREAD_LOCAL.remove()
    }

    @Test
    fun `initialises request tracing on request and sets on outgoing response when not present`() {
        var newThreadLocal: ZipkinTraces? = null
        val svc = ServerFilters.RequestTracing().then {
            newThreadLocal = ZipkinTraces.THREAD_LOCAL.get()!!
            assertThat(newThreadLocal!!.traceId, present())
            assertThat(newThreadLocal!!.spanId, present())
            assertThat(newThreadLocal!!.parentSpanId, absent())
            assertThat(newThreadLocal!!.samplingDecision, equalTo(SAMPLE))

            val setOnRequest = ZipkinTraces(it)
            assertThat(setOnRequest.traceId, equalTo(newThreadLocal!!.traceId))
            assertThat(setOnRequest.spanId, equalTo(newThreadLocal!!.spanId))
            assertThat(setOnRequest.parentSpanId, absent())
            assertThat(setOnRequest.samplingDecision, equalTo(newThreadLocal!!.samplingDecision))
            Response(OK)
        }

        val received = ZipkinTraces(svc(Request(GET, "")))

        assertThat(received, equalTo(ZipkinTraces(newThreadLocal!!.traceId, newThreadLocal!!.spanId, null, SAMPLE)))
    }

    @Test
    fun `uses existing request tracing from request and sets on outgoing response`() {
        val originalTraceId = TraceId("originalTrace")
        val originalSpanId = TraceId("originalSpan")
        val originalParentSpanId = TraceId("originalParentSpanId")
        val originalTraces = ZipkinTraces(originalTraceId, originalSpanId, originalParentSpanId, DO_NOT_SAMPLE)

        var start: Pair<Request, ZipkinTraces>? = null
        var end: Triple<Request, Response, ZipkinTraces>? = null

        val svc = ServerFilters.RequestTracing(
            { req, trace -> start = req to trace },
            { req, resp, trace -> end = Triple(req, resp, trace) }
        ).then {
            val actual = ZipkinTraces.THREAD_LOCAL.get()
            val setOnRequest = ZipkinTraces(it)

            assertThat(actual, equalTo(originalTraces))
            assertThat(setOnRequest, equalTo(originalTraces))
            Response(OK)
        }

        val originalRequest = ZipkinTraces(originalTraces, Request(GET, ""))
        val actual = svc(originalRequest)
        assertThat(ZipkinTraces(actual), equalTo(originalTraces))

        assertThat(start!!.first, equalTo(originalRequest))
        assertThat(start!!.second, equalTo(originalTraces))

        assertThat(end!!.first, equalTo(originalRequest))
        assertThat(end!!.second, equalTo(ZipkinTraces(originalTraces, Response(OK))))
        assertThat(end!!.third, equalTo(originalTraces))
    }

    @Test
    fun `GET - Cors headers are set correctly`() {
        val handler = ServerFilters.Cors(UnsafeGlobalPermissive).then { Response(I_M_A_TEAPOT) }
        val response = handler(Request(GET, "/"))

        assertThat(response, hasStatus(I_M_A_TEAPOT)
            .and(hasHeader("access-control-allow-origin", "*"))
            .and(hasHeader("access-control-allow-headers", "content-type"))
            .and(hasHeader("access-control-allow-methods", "GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE, HEAD"))
            .and(hasHeader("access-control-allow-credentials", "true")))
    }

    @Test
    fun `OPTIONS - requests are intercepted and returned with expected headers`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.AnyOf("foo", "bar"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/").header("Origin", "foo"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "foo"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `OPTIONS - requests are returned with expected headers when origin does not match`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.AnyOf("foo", "bar"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/").header("Origin", "baz"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "null"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `OPTIONS - requests are returned with expected headers when origin is not set`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.AnyOf("foo", "bar"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "null"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `OPTIONS - requests are returned with expected headers when AllowAll OriginPolicy is used`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.AllowAll(), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/").header("Origin", "foo"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "*"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `OPTIONS - requests are returned with expected headers when Only OriginPolicy is used`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.Only("foo"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/").header("Origin", "foo"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "foo"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `OPTIONS - requests are returned with expected headers when AnyOf OriginPolicy is used`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.AnyOf(listOf("foo", "bar")), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/").header("Origin", "bar"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "bar"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `OPTIONS - requests are returned with expected headers when Pattern OriginPolicy is used`() {
        val handler = ServerFilters.Cors(CorsPolicy(OriginPolicy.Pattern(Regex(".*.bar")), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/").header("Origin", "foo.bar"))

        assertThat(response, hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "foo.bar"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
            .and(!hasHeader("access-control-allow-credentials")))
    }

    @Test
    fun `catch all exceptions`() {
        val e = RuntimeException("boom!")
        val handler = ServerFilters.CatchAll(I_M_A_TEAPOT).then { throw e }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))

        assertThat(response, hasStatus(I_M_A_TEAPOT).and(hasBody(sw.toString())))
    }

    @Test
    fun `copy headers from request to response`() {
        val handler = ServerFilters.CopyHeaders("foo", "bar").then { Response(OK) }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        assertThat(response, hasHeader("foo", "one"))
        assertThat(response, hasHeader("bar", "two"))
    }

    @Test
    fun `copy only headers specified in filter`() {
        val handler = ServerFilters.CopyHeaders("a", "b").then { Response(OK) }

        val response = handler(Request(GET, "/").header("b", "2").header("c", "3"))

        assertThat(response.headers, equalTo(listOf("b" to "2") as Headers))
    }

    @Nested
    inner class GzipFilters {
        @Test
        fun `gunzip request and gzip response`() {
            val handler = ServerFilters.GZip().then {
                assertThat(it, hasBody(equalTo("hello")))
                Response(OK).body(it.body)
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped().body)),
                hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello").gzipped().body))))
        }

        @Test
        fun `handle empty messages with incorrect content-encoding`() {
            val handler = ServerFilters.GZip().then {
                assertThat(it, hasBody(equalTo(Body.EMPTY)))
                Response(OK).body(it.body)
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body.EMPTY)),
                hasBody(equalTo(Body.EMPTY)).and(!hasHeader("content-encoding", "gzip")))
        }

        @Test
        fun `passes through non-gzipped request`() {
            val handler = ServerFilters.GZip().then {
                assertThat(it, hasBody("hello"))
                Response(OK).body("hello")
            }

            handler(Request(GET, "/").body("hello"))
        }

        @Test
        fun `gunzip request and gzip response with matching content type`() {
            val handler = ServerFilters.GZipContentTypes(setOf(ContentType.TEXT_PLAIN)).then {
                assertThat(it, hasBody(equalTo("hello")))
                Response(OK).header("content-type", "text/plain").body(it.body)
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped().body)),
                hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello").gzipped().body))))
        }

        @Test
        fun `gunzip request and do not gzip response with unmatched content type`() {
            val handler = ServerFilters.GZipContentTypes(setOf(TEXT_HTML)).then {
                assertThat(it, hasBody(equalTo("hello")))
                Response(OK).header("content-type", "text/plain").body(it.body)
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped().body)),
                !hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello")))))
        }

        @Test
        fun `passes through non-gzipped request despite content type`() {
            val handler = ServerFilters.GZipContentTypes(setOf(TEXT_HTML)).then {
                assertThat(it, hasBody("hello"))
                Response(OK).body("hello")
            }

            handler(Request(GET, "/").body("hello"))
        }
    }

    @Nested
    inner class GzipStreamFilters {
        @Test
        fun `gunzip request and gzip response`() {
            val handler = ServerFilters.GZip(Streaming).then {
                assertThat(it, hasBody(equalTo("hello")))
                Response(OK).body(Body("hello"))
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped().body)),
                hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello").gzippedStream().body))))
        }

        @Test
        fun `handle empty messages with incorrect content-encoding`() {
            val handler = ServerFilters.GZip(Streaming).then {
                assertThat(it, hasBody(equalTo(Body.EMPTY)))
                Response(OK).body(it.body)
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body.EMPTY)),
                hasBody(equalTo(Body.EMPTY)).and(!hasHeader("content-encoding", "gzip")))
        }

        @Test
        fun `passes through non-gzipped request`() {
            val handler = ServerFilters.GZip(Streaming).then {
                assertThat(it, hasBody("hello"))
                Response(OK).body("hello")
            }

            handler(Request(GET, "/").body("hello"))
        }

        @Test
        fun `gunzip request and gzip response with matching content type`() {
            val handler = ServerFilters.GZipContentTypes(setOf(ContentType.TEXT_PLAIN), Streaming).then {
                assertThat(it, hasBody(equalTo("hello")))
                Response(OK).header("content-type", "text/plain").body(Body("hello"))
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped().body)),
                hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello").gzippedStream().body))))
        }

        @Test
        fun `gunzip request and do not gzip response with unmatched content type`() {
            val handler = ServerFilters.GZipContentTypes(setOf(TEXT_HTML), Streaming).then {
                assertThat(it, hasBody(equalTo("hello")))
                Response(OK).header("content-type", "text/plain").body(it.body)
            }

            assertThat(handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped().body)),
                !hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello")))))
        }

        @Test
        fun `passes through non-gzipped request despite content type`() {
            val handler = ServerFilters.GZipContentTypes(setOf(TEXT_HTML), Streaming).then {
                assertThat(it, hasBody("hello"))
                Response(OK).body("hello")
            }

            handler(Request(GET, "/").body("hello"))
        }
    }

    @Test
    fun `catch lens failure - custom response`() {
        val e = LensFailure(Invalid(Header.required("bob").meta), Missing(Header.required("bill").meta), target = Request(GET, ""))
        val handler = ServerFilters.CatchLensFailure { Response(OK).body(it.localizedMessage) }
            .then { throw e }

        val response = handler(Request(GET, "/"))

        assertThat(response, hasStatus(OK).and(hasBody("header 'bob' must be string, header 'bill' is required")))
    }

    @Test
    fun `catch lens failure - invalid`() {
        val e = LensFailure(Invalid(Header.required("bob").meta), Missing(Header.required("bill").meta), target = Request(GET, ""))
        val handler = ServerFilters.CatchLensFailure().then { throw e }

        val response = handler(Request(GET, "/"))

        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.status.description, equalTo("header 'bob' must be string; header 'bill' is required"))
    }

    @Test
    fun `catch lens failure - invalid from Response is rethrown`() {
        val e = LensFailure(Invalid(Header.required("bob").meta), Missing(Header.required("bill").meta), target = Response(OK))
        val handler = ServerFilters.CatchLensFailure().then { throw e }
        assertThat({ handler(Request(GET, "/")) }, throws(equalTo(e)))
    }

    @Test
    fun `catch lens failure - invalid from RequestContext is rethrown`() {
        val e = LensFailure(Invalid(Header.required("bob").meta), Missing(Header.required("bill").meta), target = RequestContext())
        val handler = ServerFilters.CatchLensFailure().then { throw e }
        assertThat({ handler(Request(GET, "/")) }, throws(equalTo(e)))
    }

    @Test
    fun `catch lens failure - unsupported`() {
        val e = LensFailure(Unsupported(Header.required("bob").meta), target = Request(GET, ""))
        val handler = ServerFilters.CatchLensFailure().then { throw e }

        val response = handler(Request(GET, "/"))

        assertThat(response, hasStatus(UNSUPPORTED_MEDIA_TYPE))
    }

    @Test
    fun `initialises request context for use further down the stack`() {
        val contexts = RequestContexts()
        val handler = ServerFilters.InitialiseRequestContext(contexts)
            .then(Filter { next ->
                {
                    contexts[it]["foo"] = "manchu"
                    next(it)
                }
            })
            .then { Response(OK).body(contexts[it].get<String>("foo")!!) }

        assertThat(handler(Request(GET, "/")), hasBody("manchu"))
    }

    @Test
    fun `replace response contents with static file`() {
        fun returning(status: Status) = ServerFilters.ReplaceResponseContentsWithStaticFile().then { Response(status).body(status.toString()) }

        assertThat(returning(NOT_FOUND)(Request(GET, "/")), hasBody("404 contents"))
        assertThat(returning(OK)(Request(GET, "/")), hasBody(OK.toString()))
    }

    @Test
    fun `set content type`() {
        val handler = ServerFilters.SetContentType(OCTET_STREAM).then { Response(OK) }

        assertThat(handler(Request(GET, "/")), hasContentType(OCTET_STREAM))
    }

}
