package com.gourame.http.core

typealias HttpHandler = (Request) -> Response

typealias Headers = Map<String, String>

data class Request(val method: Method, val uri: Uri, val headers: Headers = mapOf(), val entity: Entity = Entity.empty)

data class Response(val status: Status, val headers: Headers = mapOf(), val entity: Entity = Entity.empty)

data class Entity(val value: ByteArray) {
    constructor(value: String) : this(value.toByteArray())

    companion object {
        val empty = Entity("")
    }

    override fun equals(other: Any?): Boolean = other != null && other is Entity && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = String(value)
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

