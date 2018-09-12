package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.jsonrpc.MethodBindings.Companion.Auto
import org.http4k.jsonrpc.MethodBindings.Companion.Manual

object JsonRpc {
    fun <ROOT : NODE, NODE : Any> auto(json: JsonLibAutoMarshallingJson<ROOT>,
                                       errorHandler: ErrorHandler = defaultErrorHandler,
                                       fn: Auto<ROOT>.() -> Unit): JsonRpcService<ROOT, ROOT> =
            JsonRpcService(json, errorHandler, Auto(json).apply(fn))

    fun <ROOT : NODE, NODE : Any> manual(json: Json<ROOT, NODE>,
                                         errorHandler: ErrorHandler = defaultErrorHandler,
                                         fn: Manual<ROOT, NODE>.() -> Unit): JsonRpcService<ROOT, NODE> =
            JsonRpcService(json, errorHandler, Manual(json).apply(fn))
}

typealias JsonRpcHandler<IN, OUT> = (IN) -> OUT

private val defaultErrorHandler: ErrorHandler = { null }