package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson

interface MethodMappingsBuilder<ROOT: NODE, NODE> {
    fun mappings(): List<MethodMapping<NODE, NODE>>
    fun method(name: String, handler: JsonRpcHandler<NODE, NODE>)
    val json: Json<ROOT, NODE>
}

open class ManualMethodMappingsBuilder<ROOT : NODE, NODE: Any>(override val json: Json<ROOT, NODE>) :
        MethodMappingsBuilder<ROOT, NODE> {
    private val methodMappings = mutableMapOf<String, JsonRpcHandler<NODE, NODE>>()

    final override fun method(name: String, handler: JsonRpcHandler<NODE, NODE>) {
        methodMappings[name] = handler
    }

    final override fun mappings() = methodMappings.map { MethodMapping(it.key, it.value) }

    fun <IN, OUT> handler(paramsLens: Params<NODE, IN>,
                          resultLens: Result<OUT, NODE>,
                          fn: (IN) -> OUT): JsonRpcHandler<NODE, NODE> =
            handler(emptySet(), paramsLens, resultLens, fn)

    fun <IN, OUT> handler(paramsFieldNames: Set<String>,
                          paramsLens: Params<NODE, IN>,
                          resultLens: Result<OUT, NODE>,
                          fn: (IN) -> OUT): JsonRpcHandler<NODE, NODE> =
            ParamMappingJsonRequestHandler(json, paramsFieldNames, paramsLens, fn, resultLens)

    fun <OUT> handler(resultLens: Result<OUT, NODE>, block: () -> OUT): JsonRpcHandler<NODE, NODE> =
            NoParamsJsonRequestHandler(block, resultLens)
}

class AutoMethodMappingsBuilder<ROOT : Any>(override val json: JsonLibAutoMarshallingJson<ROOT>) :
        ManualMethodMappingsBuilder<ROOT, ROOT>(json) {

    inline fun <reified IN : Any, OUT : Any> handler(paramsFieldNames: Set<String>,
                                                     noinline fn: (IN) -> OUT): JsonRpcHandler<ROOT, ROOT> =
            handler(paramsFieldNames, Params { json.asA(it, IN::class) }, Result { json.asJsonObject(it) }, fn)

    inline fun <reified IN : Any, OUT : Any> handler(noinline block: (IN) -> OUT): JsonRpcHandler<ROOT, ROOT> =
            handler(IN::class.javaObjectType.declaredFields.map { it.name }.toSet(), block)

    fun <OUT : Any> handler(block: () -> OUT): JsonRpcHandler<ROOT, ROOT> =
            handler(Result { json.asJsonObject(it) }, block)
}