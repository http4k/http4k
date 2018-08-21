package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson

fun <ROOT : NODE, NODE> jsonRpc(json: Json<ROOT, NODE>,
                                definitions: ManualMethodMappingsBuilder<ROOT, NODE>.() -> Unit): JsonRpcService<ROOT, NODE> =
        jsonRpc(manual(json), definitions)

fun <ROOT : NODE, NODE, M: MethodMappingsBuilder<ROOT, NODE>> jsonRpc(
                                methodMappingsBuilder: M,
                                definitions: M.() -> Unit): JsonRpcService<ROOT, NODE> =
        JsonRpcService(methodMappingsBuilder.json, methodMappingsBuilder.apply(definitions).mappings())

fun <ROOT: NODE, NODE> manual(json: Json<ROOT, NODE>) = ManualMethodMappingsBuilder(json)

fun <ROOT: Any> auto(json: JsonLibAutoMarshallingJson<ROOT>) = AutoMethodMappingsBuilder(json)

