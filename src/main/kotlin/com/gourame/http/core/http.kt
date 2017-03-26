package com.gourame.http.core

import com.gourame.http.core.Entity.Companion.EMPTY
import com.gourame.http.core.Method.GET
import com.gourame.http.core.Method.POST
import com.gourame.http.core.Uri.Companion.uri

typealias HttpHandler = (Request) -> Response

typealias Headers = Map<String, String>

data class Request(val method: Method, val uri: Uri, val headers: Headers = mapOf(), val entity: Entity = EMPTY) {
    companion object {
        fun get(uri: String, headers: Headers = mapOf(), entity: Entity = EMPTY) = Request(GET, uri(uri), headers, entity)
        fun post(uri: String, headers: Headers = mapOf(), entity: Entity = EMPTY) = Request(POST, uri(uri), headers, entity)
    }
}

data class Response(val status: Status, val headers: Headers = mapOf(), val entity: Entity = EMPTY)

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

