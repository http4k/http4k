package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.format.JsonType.Array
import org.http4k.format.JsonType.Integer
import org.http4k.format.JsonType.Null
import org.http4k.format.JsonType.Number
import org.http4k.format.JsonType.Object

class JsonRpcResult<NODE>(json: Json<NODE>, fields: Map<String, NODE>) {
    private var valid = (fields["jsonrpc"] ?: json.nullNode()).let {
        json.typeOf(it) == JsonType.String && jsonRpcVersion == json.text(it)
    }

    val result: NODE? = fields["result"]?.also {
        if (!setOf(Object, Array).contains(json.typeOf(it))) valid = false
    }

    val error: NODE? = fields["error"]

    fun isError() = error!= null

    val id: NODE? = fields["id"]?.let {
        if (!setOf(JsonType.String, Number, Integer, Null).contains(json.typeOf(it))) {
            valid = false
            json.nullNode()
        } else it
    }
}
