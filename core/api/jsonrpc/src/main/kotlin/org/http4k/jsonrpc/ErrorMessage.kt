package org.http4k.jsonrpc

import org.http4k.format.Json

open class ErrorMessage(open val code: Int, open val message: String) {
    open fun <NODE> data(json: Json<NODE>): NODE? = null

    operator fun <NODE> invoke(json: Json<NODE>): NODE = json {
        val fields = listOf("code" to number(code), "message" to string(message))
        val data = data(json)
        json.obj(data?.let { fields + ("data" to it) } ?: fields)
    }

    override fun toString() = "ErrorMessage($code, $message)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ErrorMessage

        if (code != other.code) return false
        if (message != other.message) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code
        result = 31 * result + message.hashCode()
        return result
    }

    companion object {
        val InvalidRequest = ErrorMessage(-32600, "Invalid Request")
        val MethodNotFound = ErrorMessage(-32601, "Method not found")
        val InvalidParams = ErrorMessage(-32602, "Invalid params")
        val InternalError = ErrorMessage(-32603, "Internal error")
        val ParseError = ErrorMessage(-32700, "Parse error")
    }
}
