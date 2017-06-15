package org.http4k.filter

import org.http4k.base64Decoded
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import java.io.PrintWriter
import java.io.StringWriter

data class CorsPolicy(val origins: List<String>,
                      val headers: List<String>,
                      val methods: List<Method>) {

    companion object {
        val UnsafeGlobalPermissive = CorsPolicy(listOf("*"), listOf("content-type"), Method.values().toList())
    }
}

object ServerFilters {

    object Cors {
        private fun List<String>.joined() = this.joinToString(", ")

        operator fun invoke(policy: CorsPolicy) = Filter {
            next ->
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

    object RequestTracing {
        operator fun invoke(
            startReportFn: (Request, ZipkinTraces) -> Unit = { _, _ -> },
            endReportFn: (Request, Response, ZipkinTraces) -> Unit = { _, _, _ -> }): Filter = Filter {
            next ->
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

    object BasicAuth {
        operator fun invoke(realm: String, authorize: (Credentials) -> Boolean): Filter = Filter {
            next ->
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

    object CatchLensFailure : Filter {
        override fun invoke(next: HttpHandler): HttpHandler = {
            try {
                next(it)
            } catch (lensFailure: LensFailure) {
                Response(lensFailure.status)
            }
        }
    }

    object CatchAll {
        operator fun invoke(errorStatus: Status = INTERNAL_SERVER_ERROR): Filter = Filter {
            next ->
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

    object CopyHeaders {
        operator fun invoke(vararg headers: String): Filter = Filter {
            next ->
            {
                request ->
                val response = next(request)
                headers.fold(response,
                    { memo, name -> request.header(name)?.let { memo.header(name, it) } ?: memo })
            }
        }
    }
}