package org.http4k.jsonrpc

import org.http4k.format.Json


fun <NODE> Json<NODE>.renderResult(result: NODE, id: NODE): NODE = this {
    obj(
        "jsonrpc" to string(jsonRpcVersion),
        "result" to result,
        "id" to id
    )
}

fun <NODE> Json<NODE>.renderError(errorMessage: ErrorMessage, id: NODE? = null) = this {
    obj(
        "jsonrpc" to string(jsonRpcVersion),
        "error" to errorMessage(this),
        "id" to (id ?: nullNode())
    )
}
