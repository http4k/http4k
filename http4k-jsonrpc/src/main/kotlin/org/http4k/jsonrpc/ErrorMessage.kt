package org.http4k.jsonrpc

import org.http4k.format.Json

open class ErrorMessage(val code: Int, val message: String) {
    open fun <NODE : Any> data(json: Json<NODE>): NODE? = null

    operator fun <NODE : Any> invoke(json: Json<NODE>): NODE = json {
        val fields = listOf("code" to number(code), "message" to string(message))
        val data = data(json)
        json.obj(data?.let { fields + ("data" to it) } ?: fields)
    }

    companion object {
        val InvalidRequest = ErrorMessage(-32600, "Invalid Request")
        val MethodNotFound = ErrorMessage(-32601, "Method not found")
        val InvalidParams = ErrorMessage(-32602, "Invalid params")
        val InternalError = ErrorMessage(-32603, "Internal error")
        val ParseError = ErrorMessage(-32700, "Parse error")
    }
}
