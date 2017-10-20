package org.http4k.filter

import org.http4k.base64Decoded
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

data class CorsPolicy(val origins: List<String>,
                      val headers: List<String>,
                      val methods: List<Method>) {

    companion object {
        val UnsafeGlobalPermissive = CorsPolicy(listOf("*"), listOf("content-type"), Method.values().toList())
    }
}

object ServerFilters {

    /**
     * Add Cors headers to the Response, according to the passed CorsPolicy
     */
    object Cors {
        private fun List<String>.joined() = this.joinToString(", ")

        operator fun invoke(policy: CorsPolicy) = Filter { next ->
            {
                val response = if (it.method == OPTIONS) Response(OK) else next(it)
                response.with(
                    Header.required("access-control-allow-origin") of policy.origins.joined(),
                    Header.required("access-control-allow-headers") of policy.headers.joined(),
                    Header.required("access-control-allow-methods") of policy.methods.map { it.name }.joined()
                )
            }
        }
    }

    /**
     * Adds Zipkin request tracing headers to the incoming request and outbound response. (traceid, spanid, parentspanid)
     */
    object RequestTracing {
        operator fun invoke(
            startReportFn: (Request, ZipkinTraces) -> Unit = { _, _ -> },
            endReportFn: (Request, Response, ZipkinTraces) -> Unit = { _, _, _ -> }): Filter = Filter { next ->
            {
                val fromRequest = ZipkinTraces(it)
                startReportFn(it, fromRequest)
                ZipkinTraces.THREAD_LOCAL.set(fromRequest.copy(parentSpanId = fromRequest.spanId, spanId = TraceId.new()))

                try {
                    val response = ZipkinTraces(fromRequest, next(ZipkinTraces(fromRequest, it)))
                    endReportFn(it, response, fromRequest)
                    response
                } finally {
                    ZipkinTraces.THREAD_LOCAL.remove()
                }
            }

        }
    }

    /**
     * Simple Basic Auth credential checking.
     */
    object BasicAuth {
        operator fun invoke(realm: String, authorize: (Credentials) -> Boolean): Filter = Filter { next ->
            {
                val credentials = it.basicAuthenticationCredentials()
                if (credentials == null || !authorize(credentials)) {
                    Response(UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
                } else next(it)
            }
        }


        operator fun invoke(realm: String, user: String, password: String): Filter = this(realm, Credentials(user, password))
        operator fun invoke(realm: String, credentials: Credentials): Filter = this(realm, { it == credentials })

        private fun Request.basicAuthenticationCredentials(): Credentials? = header("Authorization")?.replace("Basic ", "")?.toCredentials()

        private fun String.toCredentials(): Credentials? = base64Decoded().split(":").let { Credentials(it.getOrElse(0, { "" }), it.getOrElse(1, { "" })) }
    }

    /**
     * Converts Lens extraction failures into correct HTTP responses (Bad Requests/UnsupportedMediaType).
     * This is required when using lenses to automatically unmarshall inbound requests.
     * Note that LensFailures from unmarshalling upstream Response objects are NOT caught to avoid incorrect server behaviour.
     */
    object CatchLensFailure : Filter {
        override fun invoke(next: HttpHandler): HttpHandler = {
            try {
                next(it)
            } catch (lensFailure: LensFailure) {
                when {
                    lensFailure.clazz == Response::class.java -> throw lensFailure
                    lensFailure.overall() == Failure.Type.Unsupported -> Response(UNSUPPORTED_MEDIA_TYPE)
                    else -> Response(BAD_REQUEST.description(lensFailure.failures.joinToString("; ")))
                }
            }
        }
    }

    /**
     * Last gasp filter which catches all exceptions and returns a formatted Internal Server Error.
     */
    object CatchAll {
        operator fun invoke(errorStatus: Status = INTERNAL_SERVER_ERROR): Filter = Filter { next ->
            {
                try {
                    next(it)
                } catch (e: Exception) {
                    val sw = StringWriter()
                    e.printStackTrace(PrintWriter(sw))
                    Response(errorStatus).body(sw.toString())
                }
            }
        }

    }

    /**
     * Copy headers from the incoming request to the outbound response.
     */
    object CopyHeaders {
        operator fun invoke(vararg headers: String): Filter = Filter { next ->
            { request ->
                headers.fold(next(request),
                    { memo, name -> request.header(name)?.let { memo.header(name, it) } ?: memo })
            }
        }
    }

    /**
     * Basic GZip and Gunzip support of Request/Response. Does not currently support GZipping streams.
     * Only Gunzips requests which contain "transfer-encoding" header containing 'gzip'
     * Only Gzips responses when request contains "accept-encoding" header containing 'gzip'.
     */
    object GZip {
        operator fun invoke(): Filter = RequestFilters.GunZip().then(ResponseFilters.GZip())
    }

    /**
     * Initialise a RequestContext for each request which passes through the Filter stack, and remove the
     */
    object InitialiseRequestContext {
        operator fun invoke(contexts: RequestContexts): Filter = Filter { next ->
            {
                val context = RequestContext(UUID.randomUUID())
                try {
                    next(contexts.inject(context, it))
                } finally {
                    contexts.remove(context)
                }
            }
        }
    }
}