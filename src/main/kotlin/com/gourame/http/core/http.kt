package com.gourame.http.core

typealias HttpHandler = (Request) -> Response

data class Request(val method: Method, val uri: Uri, val headers: Headers = mapOf(), val entity: Entity = Entity.empty)typealias Headers = Map<String, String>

data class Response(val status: Status, val headers: Headers = mapOf(), val entity: Entity = Entity.empty)

data class Entity(val value: Any) {
    companion object {
        val empty = Entity("")
    }

    override fun toString(): String = when (value) {
        is ByteArray -> String(value)
        else -> value.toString()
    }
}

data class Uri(val uri: String) {
    constructor(scheme: String, authority: String, path: String, query: String, fragment: String) : this("ignored")

    override fun toString(): String = uri
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

data class Status(val code: Int, val description: String) {
    companion object {
        val OK = Status(200, "OK")
    }
}