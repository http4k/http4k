package org.http4k.filter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.I_M_A_TEAPOT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
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
            newThreadLocal!!.traceId shouldMatch present()
            newThreadLocal!!.spanId shouldMatch present()
            newThreadLocal!!.parentSpanId shouldMatch absent()
            newThreadLocal!!.samplingDecision shouldMatch equalTo(SAMPLE)

            val setOnRequest = ZipkinTraces(it)
            setOnRequest.traceId shouldMatch equalTo(newThreadLocal!!.traceId)
            setOnRequest.spanId shouldMatch equalTo(newThreadLocal!!.spanId)
            setOnRequest.parentSpanId shouldMatch absent()
            setOnRequest.samplingDecision shouldMatch equalTo(newThreadLocal!!.samplingDecision)
            Response(OK)
        }

        val received = ZipkinTraces(svc(Request(GET, "")))

        received shouldMatch equalTo(ZipkinTraces(newThreadLocal!!.traceId, newThreadLocal!!.spanId, null, SAMPLE))
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

            actual shouldMatch equalTo(originalTraces)
            setOnRequest shouldMatch equalTo(originalTraces)
            Response(OK)
        }

        val originalRequest = ZipkinTraces(originalTraces, Request(GET, ""))
        val actual = svc(originalRequest)
        ZipkinTraces(actual) shouldMatch equalTo(originalTraces)

        start!!.first shouldMatch equalTo(originalRequest)
        start!!.second shouldMatch equalTo(originalTraces)

        end!!.first shouldMatch equalTo(originalRequest)
        end!!.second shouldMatch equalTo(ZipkinTraces(originalTraces, Response(OK)))
        end!!.third shouldMatch equalTo(originalTraces)
    }

    @Test
    fun `GET - Cors headers are set correctly`() {
        val handler = ServerFilters.Cors(UnsafeGlobalPermissive).then { Response(I_M_A_TEAPOT) }
        val response = handler(Request(GET, "/"))

        response shouldMatch hasStatus(I_M_A_TEAPOT)
            .and(hasHeader("access-control-allow-origin", "*"))
            .and(hasHeader("access-control-allow-headers", "content-type"))
            .and(hasHeader("access-control-allow-methods", "GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE, HEAD"))
    }

    @Test
    fun `OPTIONS - requests are intercepted and returned with expected headers`() {
        val handler = ServerFilters.Cors(CorsPolicy(listOf("foo", "bar"), listOf("rita", "sue", "bob"), listOf(DELETE, POST))).then { Response(INTERNAL_SERVER_ERROR) }
        val response = handler(Request(OPTIONS, "/"))

        response shouldMatch hasStatus(OK)
            .and(hasHeader("access-control-allow-origin", "foo, bar"))
            .and(hasHeader("access-control-allow-headers", "rita, sue, bob"))
            .and(hasHeader("access-control-allow-methods", "DELETE, POST"))
    }

    @Test
    fun `catch all exceptions`() {
        val e = RuntimeException("boom!")
        val handler = ServerFilters.CatchAll(I_M_A_TEAPOT).then { throw e }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))

        response shouldMatch hasStatus(I_M_A_TEAPOT).and(hasBody(sw.toString()))
    }

    @Test
    fun `copy headers from request to response`() {
        val handler = ServerFilters.CopyHeaders("foo", "bar").then { Response(OK) }

        val response = handler(Request(GET, "/").header("foo", "one").header("bar", "two"))

        response shouldMatch hasHeader("foo", "one")
        response shouldMatch hasHeader("bar", "two")
    }

    @Test
    fun `copy only headers specified in filter`() {
        val handler = ServerFilters.CopyHeaders("a", "b").then { Response(OK) }

        val response = handler(Request(GET, "/").header("b", "2").header("c", "3"))

        assertThat(response.headers, equalTo(listOf("b" to "2") as Headers))
    }

    @Test
    fun `gunzip request and gzip response`() {
        val handler = ServerFilters.GZip().then {
            it shouldMatch hasBody(equalTo("hello"))
            Response(OK).body(it.body)
        }

        handler(Request(GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped())) shouldMatch
            hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello").gzipped())))
    }

    @Test
    fun `passes through non-gzipped request`() {
        val handler = ServerFilters.GZip().then {
            it shouldMatch hasBody("hello")
            Response(OK).body("hello")
        }

        handler(Request(GET, "/").body("hello"))
    }

    @Test
    fun `gunzip request and gzip response with matching content type`() {
        val handler = ServerFilters.GZipContentTypes(setOf(ContentType.TEXT_PLAIN)).then {
            it shouldMatch hasBody(equalTo("hello"))
            Response(OK).header("content-type", "text/plain").body(it.body)
        }

        handler(Request(Method.GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped())) shouldMatch
                hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello").gzipped())))
    }

    @Test
    fun `gunzip request and do not gzip response with unmatched content type`() {
        val handler = ServerFilters.GZipContentTypes(setOf(ContentType.TEXT_HTML)).then {
            it shouldMatch hasBody(equalTo("hello"))
            Response(OK).header("content-type", "text/plain").body(it.body)
        }

        handler(Request(Method.GET, "/").header("accept-encoding", "gzip").header("content-encoding", "gzip").body(Body("hello").gzipped())) shouldMatch
                !hasHeader("content-encoding", "gzip").and(hasBody(equalTo(Body("hello"))))
    }

    @Test
    fun `passes through non-gzipped request despite content type`() {
        val handler = ServerFilters.GZipContentTypes(setOf(TEXT_HTML)).then {
            it shouldMatch hasBody("hello")
            Response(OK).body("hello")
        }

        handler(Request(GET, "/").body("hello"))
    }

    @Test
    fun `catch lens failure - custom response`() {
        val e = LensFailure(Invalid(Header.required("bob").meta), Missing(Header.required("bill").meta))
        val handler = ServerFilters.CatchLensFailure { Response(OK).body(it.localizedMessage) }
            .then { throw e }

        val response = handler(Request(GET, "/"))

        response shouldMatch hasStatus(OK).and(hasBody("header 'bob' must be string, header 'bill' is required"))
    }

    @Test
    fun `catch lens failure - invalid`() {
        val e = LensFailure(Invalid(Header.required("bob").meta), Missing(Header.required("bill").meta))
        val handler = ServerFilters.CatchLensFailure().then { throw e }

        val response = handler(Request(GET, "/"))

        response shouldMatch hasStatus(BAD_REQUEST)
        response.status.description shouldMatch equalTo("header 'bob' must be string; header 'bill' is required")
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
        val e = LensFailure(Unsupported(Header.required("bob").meta))
        val handler = ServerFilters.CatchLensFailure().then { throw e }

        val response = handler(Request(GET, "/"))

        response shouldMatch hasStatus(UNSUPPORTED_MEDIA_TYPE)
    }

    @Test
    fun `initialises request context for use further down the stack`() {
        val contexts = RequestContexts()
        val handler = ServerFilters.InitialiseRequestContext(contexts)
            .then(Filter { next ->
                {
                    contexts[it].set("foo", "manchu")
                    next(it)
                }
            })
            .then { Response(OK).body(contexts[it].get<String>("foo")!!) }

        handler(Request(GET, "/")) shouldMatch hasBody("manchu")
    }

    @Test
    fun `replace response contents with static file`() {
        fun returning(status: Status) = ServerFilters.ReplaceResponseContentsWithStaticFile().then { Response(status).body(status.toString()) }

        returning(NOT_FOUND)(Request(GET, "/")) shouldMatch hasBody("404 contents")
        returning(OK)(Request(GET, "/")) shouldMatch hasBody(Status.OK.toString())
    }

    @Test
    fun `set content type`() {
        val handler = ServerFilters.SetContentType(OCTET_STREAM).then { Response(OK) }

        handler(Request(GET, "/")) shouldMatch hasContentType(OCTET_STREAM)
    }

}
