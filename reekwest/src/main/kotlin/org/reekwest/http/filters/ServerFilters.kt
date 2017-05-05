package org.reekwest.http.filters

import org.reekwest.http.Credentials
import org.reekwest.http.base64Decoded
import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.lens.LensFailure

object ServerFilters {

    object RequestTracing : Filter {
        override fun invoke(next: HttpHandler): HttpHandler = {
            val fromRequest = ZipkinTraces(it)
            ZipkinTraces.THREAD_LOCAL.set(fromRequest.copy(
                parentSpanId = fromRequest.spanId,
                spanId = TraceId.new()
            ))
            try {
                ZipkinTraces(fromRequest, next(it))
            } finally {
                ZipkinTraces.THREAD_LOCAL.remove()
            }

        }
    }

    object BasicAuth {
        operator fun invoke(realm: String, authorize: (Credentials) -> Boolean): Filter = Filter {
            handler ->
            {
                val credentials = it.basicAuthenticationCredentials()
                if (credentials == null || !authorize(credentials)) {
                    Response(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
                } else {
                    handler(it)
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