package org.http4k.format

import org.http4k.core.Body
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.jsonrpc.jsonRpcVersion
import org.http4k.lens.ContentNegotiation.Companion.None

fun <NODE> Json<NODE>.renderRequest(method: String, params: NODE?, id: NODE): NODE = this {
    obj(
        "jsonrpc" to string(jsonRpcVersion),
        "method" to string(method),
        "params" to (params ?: nullNode()),
        "id" to id
    )
}

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

fun <NODE> Body.Companion.jsonRpcRequest(json: Json<NODE>) =
    json.body("body", None).map { JsonRpcRequest(json, json.fields(it).toMap()) }


fun <NODE> Body.Companion.jsonRpcResult(json: Json<NODE>) =
    json.body("body", None).map { JsonRpcResult(json, json.fields(it).toMap()) }

