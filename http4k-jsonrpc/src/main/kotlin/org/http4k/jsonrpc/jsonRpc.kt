package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson

object JsonRpc {
    fun <ROOT : NODE, NODE : Any> auto(json: JsonLibAutoMarshallingJson<ROOT>,
                                           errorHandler: ErrorHandler = defaultErrorHandler,
                                           definitions: AutoMethodMappingsBuilder<ROOT>.() -> Unit): JsonRpcService<ROOT, ROOT> =
            JsonRpcService(json, errorHandler, AutoMethodMappingsBuilder(json).apply(definitions).mappings())

    fun <ROOT : NODE, NODE: Any> manual(json: Json<ROOT, NODE>,
                                         errorHandler: ErrorHandler = defaultErrorHandler,
                                         definitions: ManualMethodMappingsBuilder<ROOT, NODE>.() -> Unit): JsonRpcService<ROOT, NODE> =
            JsonRpcService(json, errorHandler, ManualMethodMappingsBuilder(json).apply(definitions).mappings())
}

typealias JsonRpcHandler<IN, OUT> = (IN) -> OUT

private val defaultErrorHandler: ErrorHandler = { null }