package org.reekwest.http.core

import org.reekwest.http.core.Method.DELETE
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Method.OPTIONS
import org.reekwest.http.core.Method.PATCH
import org.reekwest.http.core.Method.POST
import org.reekwest.http.core.Method.PUT
import org.reekwest.http.core.Method.TRACE
import org.reekwest.http.core.Uri.Companion.uri
import java.nio.ByteBuffer

sealed class HttpMessage {
    abstract val headers: Headers
    abstract val body: Body?
}

data class Request(val method: Method, val uri: Uri, override val headers: Headers = listOf(), override val body: Body? = null) : HttpMessage() {

    companion object {
        fun get(uri: String, headers: Headers = listOf(), body: Body? = null) = get(uri(uri), headers, body)
        fun get(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(GET, uri, headers, body)

        fun post(uri: String, headers: Headers = listOf(), body: Body? = null) = post(uri(uri), headers, body)
        fun post(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(POST, uri, headers, body)

        fun put(uri: String, headers: Headers = listOf(), body: Body? = null) = put(uri(uri), headers, body)
        fun put(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(PUT, uri, headers, body)

        fun delete(uri: String, headers: Headers = listOf(), body: Body? = null) = delete(uri(uri), headers, body)
        fun delete(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(DELETE, uri, headers, body)

        fun options(uri: String, headers: Headers = listOf(), body: Body? = null) = options(uri(uri), headers, body)
        fun options(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(OPTIONS, uri, headers, body)

        fun trace(uri: String, headers: Headers = listOf(), body: Body? = null) = trace(uri(uri), headers, body)
        fun trace(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(TRACE, uri, headers, body)

        fun patch(uri: String, headers: Headers = listOf(), body: Body? = null) = patch(uri(uri), headers, body)
        fun patch(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(PATCH, uri, headers, body)

    }

    fun query(name: String, value: String) = copy(uri = uri.query(name, value))

    fun query(name: String): String? = uri.queries().findSingle(name)

    fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

    override fun toString(): String = toStringMessage()
}

data class Response(val status: Status, override val headers: Headers = listOf(), override val body: Body? = null) : HttpMessage() {
    override fun toString(): String = toStringMessage()
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

typealias HttpHandler = (Request) -> Response

typealias HttpClient = HttpHandler

typealias Headers = Parameters

typealias Body = ByteBuffer

interface Filter : (HttpHandler) -> HttpHandler {
    companion object {
        operator fun invoke(fn: (HttpHandler) -> HttpHandler): Filter = object : Filter {
            operator override fun invoke(next: HttpHandler): HttpHandler = fn(next)
        }
    }
}

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: HttpHandler): HttpHandler = { this(next)(it) }

