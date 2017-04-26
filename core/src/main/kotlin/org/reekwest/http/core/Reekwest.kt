package org.reekwest.http.core

import java.nio.ByteBuffer

sealed class HttpMessage {
    abstract val headers: Headers
    abstract val body: Body?
}

data class Request(val method: Method, val uri: Uri, override val headers: Headers = listOf(), override val body: Body? = null) : HttpMessage() {
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

