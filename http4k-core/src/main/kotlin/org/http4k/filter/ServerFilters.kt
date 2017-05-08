package org.http4k.filter

import org.http4k.Credentials
import org.http4k.base64Decoded
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.LensFailure

object ServerFilters {

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
                    val response = ZipkinTraces(fromRequest, next(it))
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
                    Response(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
                } else {
                    next(it)
                }
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
}