package org.http4k.jsonrpc

import org.http4k.format.AutoMarshallingJson
import org.http4k.format.Json
import org.http4k.jsonrpc.MethodBindings.Companion.Auto
import org.http4k.jsonrpc.MethodBindings.Companion.Manual

object JsonRpc {
    fun <NODE : Any> auto(json: AutoMarshallingJson<NODE>,
                          errorHandler: ErrorHandler = defaultErrorHandler,
                          fn: Auto<NODE>.() -> Unit): JsonRpcService<NODE> =
        JsonRpcService(json, errorHandler, Auto(json).apply(fn))

    fun <NODE : Any> manual(json: Json<NODE>,
                            errorHandler: ErrorHandler = defaultErrorHandler,
                            fn: Manual<NODE>.() -> Unit): JsonRpcService<NODE> =
        JsonRpcService(json, errorHandler, Manual(json).apply(fn))
}

typealias JsonRpcHandler<IN, OUT> = (IN) -> OUT

private val defaultErrorHandler: ErrorHandler = { null }
