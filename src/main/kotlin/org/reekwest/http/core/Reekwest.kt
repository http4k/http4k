package org.reekwest.http.core

import org.reekwest.http.core.entity.Entity
import org.reekwest.http.core.entity.StringEntity

typealias HttpHandler = (Request) -> Response

typealias Headers = Parameters

sealed class HttpMessage {
    val version = "HTTP/1.1"
    abstract val headers: Headers
    abstract val entity: Entity?
}

data class Request(val method: Method, val uri: Uri, override val headers: Headers = listOf(), override val entity: Entity? = null) : HttpMessage() {
    override fun toString(): String = listOf("$method $uri $version", headers.toMessage(), StringEntity.fromEntity(entity)).joinToString("\r\n")
}

data class Response(val status: Status, override val headers: Headers = listOf(), override val entity: Entity? = null) : HttpMessage() {
    override fun toString(): String = listOf("$version $status", headers.toMessage(), StringEntity.fromEntity(entity)).joinToString("\r\n")
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

private fun Headers.toMessage() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")