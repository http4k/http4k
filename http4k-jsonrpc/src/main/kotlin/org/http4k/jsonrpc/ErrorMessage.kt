package org.http4k.jsonrpc

import org.http4k.format.Json

open class ErrorMessage(val code: Int, val message: String): JsonNodeProducer {

    open fun <ROOT : NODE, NODE> data(json: Json<ROOT, NODE>): NODE? = null

    final override fun <ROOT : NODE, NODE> invoke(json: Json<ROOT, NODE>): NODE {
        val fields = listOf(
                "code" to json.number(code),
                "message" to json.string(message)
        )
        val data = data(json)
        return json.obj(data?.let { fields + ("data" to it) } ?: fields)
    }

    companion object {
        val InvalidRequest = ErrorMessage(-32600, "Invalid Request")
        val MethodNotFound = ErrorMessage(-32601, "Method not found")
        val InvalidParams = ErrorMessage(-32602, "Invalid params")
        val InternalError = ErrorMessage(-32603, "Internal error")
        val ParseError = ErrorMessage(-32700, "Parse error")
    }
}

interface JsonNodeProducer {
    operator fun <ROOT: NODE, NODE> invoke(json: Json<ROOT, NODE>): NODE
}