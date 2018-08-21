package org.http4k.jsonrpc

import org.http4k.format.JsonLibAutoMarshallingJson

class AutoMethodMappingsBuilder<ROOT: Any>(override val json: JsonLibAutoMarshallingJson<ROOT>) :
        ManualMethodMappingsBuilder<ROOT, ROOT>(json) {

    inline fun <reified IN: Any, OUT: Any> handler(paramsFieldNames: Set<String>,
                                                   noinline block: (IN) -> OUT): RequestHandler<ROOT, ROOT> {
        val paramsLens = Params<ROOT, IN> {
            json.asA(it, IN::class)
        }
        val resultLens = Result<OUT, ROOT> {
            json.asJsonObject(it)
        }
        return handler(paramsFieldNames, paramsLens, resultLens, block)
    }

    inline fun <reified IN: Any, OUT: Any> handler(noinline block: (IN) -> OUT): RequestHandler<ROOT, ROOT> =
            handler(emptySet(), block)

    fun <OUT: Any> handler(block: () -> OUT): RequestHandler<ROOT, ROOT> {
        val resultLens = Result<OUT, ROOT> {
            json.asJsonObject(it)
        }
        return handler(resultLens, block)
    }
}