package org.reekwest.http.core

typealias HttpHandler = (Request) -> Response

typealias Headers = Parameters

sealed class HttpMessage {
    abstract val headers: Headers
    abstract val entity: Entity?
}

data class Request(val method: Method, val uri: Uri, override val headers: Headers = listOf(), override val entity: Entity? = null) : HttpMessage()

data class Response(val status: Status, override val headers: Headers = listOf(), override val entity: Entity? = null) : HttpMessage()

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }