package org.reekwest.http.core

import org.reekwest.http.core.entity.Entity

typealias HttpHandler = (Request) -> Response

typealias Headers = Parameters

sealed class HttpMessage {
    abstract val headers: Headers
    abstract val entity: Entity?
}

data class Request(val method: Method, val uri: Uri, override val headers: Headers = listOf(), override val entity: Entity? = null) : HttpMessage() {
    override fun toString(): String = toStringMessage()
}

data class Response(val status: Status, override val headers: Headers = listOf(), override val entity: Entity? = null) : HttpMessage() {
    override fun toString(): String = toStringMessage()
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }