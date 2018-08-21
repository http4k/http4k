package org.http4k.jsonrpc

import org.http4k.format.Json

open class ManualMethodMappingsBuilder<ROOT : NODE, NODE>(override val json: Json<ROOT, NODE>) :
        MethodMappingsBuilder<ROOT, NODE> {
    private val methodMappings = mutableMapOf<String, RequestHandler<NODE, NODE>>()

    final override fun method(name: String, handler: RequestHandler<NODE, NODE>) {
        methodMappings[name] = handler
    }

    final override fun mappings() = methodMappings.map { MethodMapping(it.key, it.value) }

    fun <IN, OUT> handler(paramsLens: Params<NODE, IN>,
                          resultLens: Result<OUT, NODE>,
                          block: (IN) -> OUT): RequestHandler<NODE, NODE> =
            handler(emptySet(), paramsLens, resultLens, block)

    fun <IN, OUT> handler(paramsFieldNames: Set<String>,
                          paramsLens: Params<NODE, IN>,
                          resultLens: Result<OUT, NODE>,
                          block: (IN) -> OUT): RequestHandler<NODE, NODE> =
            ParamMappingJsonRequestHandler(json, paramsFieldNames, paramsLens, block, resultLens)

    fun <OUT> handler(resultLens: Result<OUT, NODE>, block: () -> OUT): RequestHandler<NODE, NODE> =
            NoParamsJsonRequestHandler(block, resultLens)
}