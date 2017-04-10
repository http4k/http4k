package org.reekwest.http.core

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

typealias Headers = Parameters

typealias Body = java.nio.ByteBuffer
