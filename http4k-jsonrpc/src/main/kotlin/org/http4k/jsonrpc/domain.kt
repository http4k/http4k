package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType

internal const val jsonRpcVersion: String = "2.0"

internal class JsonRpcRequest<ROOT: NODE, NODE>(json: Json<ROOT, NODE>, fields: Map<String, NODE>) {
    private var valid = (fields["jsonrpc"] ?: json.nullNode()).let {
        json.typeOf(it) == JsonType.String && jsonRpcVersion == json.text(it)
    }

    val method: String = (fields["method"] ?: json.nullNode()).let {
        if (json.typeOf(it) == JsonType.String) {
            json.text(it)
        } else {
            valid = false
            ""
        }
    }
    val params: NODE? = fields["params"]?.also {
        if (!setOf(JsonType.Object, JsonType.Array).contains(json.typeOf(it))) {
            valid = false
        }
    }

    val id: NODE? = fields["id"]?.let {
        if (!setOf(JsonType.String, JsonType.Number, JsonType.Null).contains(json.typeOf(it))) {
            valid = false
            json.nullNode()
        } else {
            it
        }
    }

    fun valid() = valid
}

data class ErrorMessage(val code: Int, val message: String) {
    companion object {
        val ServerError = ErrorMessage(-32000, "Server error")
        val InvalidRequest = ErrorMessage(-32600, "Invalid Request")
        val MethodNotFound = ErrorMessage(-32601, "Method not found")
        val InvalidParams = ErrorMessage(-32602, "Invalid params")
        val InternalError = ErrorMessage(-32603, "Internal error")
        val ParseError = ErrorMessage(-32700, "Parse error")
    }
}