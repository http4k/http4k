package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson

fun <ROOT : NODE, NODE> jsonRpc(json: Json<ROOT, NODE>,
                                definitions: ManualMethodMappingsBuilder<ROOT, NODE>.() -> Unit): JsonRpcService<ROOT, NODE> =
        jsonRpc(json, { null }, definitions)

fun <ROOT : NODE, NODE> jsonRpc(json: Json<ROOT, NODE>,
                                errorHandler: ErrorHandler,
                                definitions: ManualMethodMappingsBuilder<ROOT, NODE>.() -> Unit): JsonRpcService<ROOT, NODE> =
        jsonRpc(manual(json), errorHandler, definitions)

fun <ROOT : NODE, NODE, M: MethodMappingsBuilder<ROOT, NODE>> jsonRpc(
        methodMappingsBuilder: M,
        definitions: M.() -> Unit): JsonRpcService<ROOT, NODE> =
        jsonRpc(methodMappingsBuilder, { null }, definitions)

fun <ROOT : NODE, NODE, M: MethodMappingsBuilder<ROOT, NODE>> jsonRpc(
                                methodMappingsBuilder: M,
                                errorHandler: ErrorHandler,
                                definitions: M.() -> Unit): JsonRpcService<ROOT, NODE> =
        JsonRpcService(methodMappingsBuilder.json, errorHandler, methodMappingsBuilder.apply(definitions).mappings())

fun <ROOT: NODE, NODE> manual(json: Json<ROOT, NODE>) = ManualMethodMappingsBuilder(json)

fun <ROOT: Any> auto(json: JsonLibAutoMarshallingJson<ROOT>) = AutoMethodMappingsBuilder(json)

