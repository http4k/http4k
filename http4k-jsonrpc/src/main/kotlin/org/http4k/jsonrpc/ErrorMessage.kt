package org.http4k.jsonrpc

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