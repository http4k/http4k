package org.http4k.security.signature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpMessage
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.lens.LensExtractor
import org.http4k.security.signature.ExtractorError.MissingComponent
import org.http4k.security.signature.ExtractorError.UnsupportedComponent

/**
 * SignatureComponent is an interface that represents a component of an HTTP message that makes up a signature.
 */
sealed interface SignatureComponent<in M : HttpMessage> : LensExtractor<M, Result<ComponentValue, ExtractorError>> {
    val name: String
    val params: Map<String, String>
    val sourceName: String get() = name

    data class Header<M : HttpMessage>(
        private val rawName: String, override val params: Map<String, String> = emptyMap()
    ) : SignatureComponent<M> {
        init {
            require(!name.startsWith("@")) { "Header component name must not start with '@': $name" }
        }

        override val name: String get() = rawName.lowercase()

        override fun invoke(target: M): Result<ComponentValue, ExtractorError> =
            target.header(name)?.let(::Success) ?: Failure(MissingComponent(this))
    }

    sealed class Derived<in M : HttpMessage>(
        final override val name: String,
        override val params: Map<String, String> = emptyMap()
    ) : SignatureComponent<M> {
        init {
            require(name.startsWith("@")) { "Derived component name must start with '@': $name" }
        }
    }

    data object Authority : Derived<Request>("@authority") {
        override fun invoke(target: Request) =
            target.header("Host")?.let(::Success) ?: Failure(MissingComponent(this))
    }

    data object Method : Derived<Request>("@method") {
        override fun invoke(target: Request) = Success(target.method.toString())
    }

    data object Path : Derived<Request>("@path") {
        override fun invoke(target: Request) = Success(target.uri.path)
    }

    data object Query : Derived<Request>("@query") {
        override fun invoke(target: Request) = Success(
            when {
                target.uri.query.isNotEmpty() -> "?${target.uri.query}"
                else -> ""
            }
        )
    }

    class QueryParam(paramName: String) : Derived<Request>("@query-param", mapOf("name" to paramName)) {
        override fun invoke(target: Request): Result<ComponentValue, ExtractorError> {
            val paramName = params["name"] ?: return Failure(UnsupportedComponent(this))

            val queryParams = target.queries(paramName)

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

    data object RequestTarget : Derived<Request>("@request-target") {
        override fun invoke(target: Request) = Success(
            when {
                target.method.toString() == "CONNECT" -> "${target.uri.host}:${target.uri.port ?: 443}"
                target.method == OPTIONS && target.uri.path == "*" -> "*"
                else -> {
                    val path = target.uri.path.ifEmpty { "/" }
                    val query = if (target.uri.query.isNotEmpty()) "?${target.uri.query}" else ""
                    "$path$query"
                }
            }
        )
    }

    data object Scheme : Derived<Request>("@scheme") {
        override fun invoke(target: Request) = target.uri.scheme.let { Success(it.lowercase()) }
    }

    data object Status : Derived<Response>("@status") {
        override fun invoke(target: Response) = Success(target.status.code.toString())
    }

    data object TargetUri : Derived<Request>("@target-uri") {
        override fun invoke(target: Request) = Success(target.uri.toString())
    }
}

sealed class ExtractorError {
    data class MissingComponent(val component: SignatureComponent<*>) : ExtractorError()
    data class UnsupportedComponent(val component: SignatureComponent<*>) : ExtractorError()
}
