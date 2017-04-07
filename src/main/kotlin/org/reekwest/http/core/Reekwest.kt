package org.reekwest.http.core

import org.reekwest.http.core.body.Body


typealias HttpHandler = (Request) -> Response

typealias Headers = Parameters

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
