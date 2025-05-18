package org.http4k.security.signature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpMessage
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.signature.ExtractorError.MissingComponent
import org.http4k.security.signature.ExtractorError.UnsupportedComponent

/**
 * SignatureComponent is an interface that represents a component of an HTTP message that makes up a signature.
 */
sealed interface SignatureComponent<in Target : HttpMessage> {
    val name: String
    val params: Map<String, String>

    /**
     * Extract the component to be signed from the request and target message (these can be the same thing).
     */
    operator fun invoke(request: Request, target: Target): Result<ComponentValue, ExtractorError>

    data class Header<Target : HttpMessage>(
        private val rawName: String, override val params: Map<String, String> = emptyMap()
    ) : SignatureComponent<Target> {
        init {
            require(!name.startsWith("@")) { "Header component name must not start with '@': $name" }
        }

        override val name: String get() = rawName.lowercase()

        override operator fun invoke(request: Request, target: Target): Result<ComponentValue, ExtractorError> =
            request.header(name)?.let(::Success) ?: Failure(MissingComponent(this))
    }

    sealed class Derived<in Target : HttpMessage>(
        final override val name: String,
        override val params: Map<String, String> = emptyMap()
    ) : SignatureComponent<Target> {
        init {
            require(name.startsWith("@")) { "Derived component name must start with '@': $name" }
        }
    }

    data object Authority : Derived<HttpMessage>("@authority") {
        override operator fun invoke(request: Request, target: HttpMessage) =
            request.header("Host")?.let(::Success) ?: Failure(MissingComponent(this))
    }

    data object Method : Derived<HttpMessage>("@method") {
        override operator fun invoke(request: Request, target: HttpMessage) = Success(request.method.toString())
    }

    data object Path : Derived<HttpMessage>("@path") {
        override operator fun invoke(request: Request, target: HttpMessage) = Success(request.uri.path)
    }

    data object Query : Derived<HttpMessage>("@query") {
        override operator fun invoke(request: Request, target: HttpMessage) = Success(
            when {
                request.uri.query.isNotEmpty() -> "?${request.uri.query}"
                else -> ""
            }
        )
    }

    class QueryParam(paramName: String) : Derived<HttpMessage>("@query-param", mapOf("name" to paramName)) {
        override operator fun invoke(request: Request, target: HttpMessage): Result<ComponentValue, ExtractorError> {
            val paramName = params["name"] ?: return Failure(UnsupportedComponent(this))

            val queryParams = request.queries(paramName)

            return when {
                queryParams.isEmpty() -> Failure(MissingComponent(this))
                queryParams.size > 1 -> {
                    // RFC 9421 says: "If a parameter name occurs multiple times in a request,
                    // the named query parameter MUST NOT be included."
                    Failure(UnsupportedComponent(this))
                }

                else -> queryParams.first()?.let { Success(it) } ?: Failure(UnsupportedComponent(this))
            }
        }
    }

    data object RequestTarget : Derived<HttpMessage>("@request-target") {
        override operator fun invoke(request: Request, target: HttpMessage) = Success(
            when {
                request.method.toString() == "CONNECT" -> "${request.uri.host}:${request.uri.port ?: 443}"
                request.method == OPTIONS && request.uri.path == "*" -> "*"
                else -> {
                    val path = request.uri.path.ifEmpty { "/" }
                    val query = if (request.uri.query.isNotEmpty()) "?${request.uri.query}" else ""
                    "$path$query"
                }
            }
        )
    }

    data object Scheme : Derived<HttpMessage>("@scheme") {
        override operator fun invoke(request: Request, target: HttpMessage) =
            request.uri.scheme.let { Success(it.lowercase()) }
    }

    data object Status : Derived<Response>("@status") {
        override operator fun invoke(request: Request, target: Response) = Success(target.status.code.toString())
    }

    data object TargetUri : Derived<HttpMessage>("@target-uri") {
        override operator fun invoke(request: Request, target: HttpMessage) = Success(request.uri.toString())
    }
}

sealed class ExtractorError {
    data class MissingComponent(val component: SignatureComponent<*>) : ExtractorError()
    data class UnsupportedComponent(val component: SignatureComponent<*>) : ExtractorError()
}
