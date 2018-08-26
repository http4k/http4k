package org.http4k.jsonrpc

import org.http4k.format.JsonLibAutoMarshallingJson

class AutoMethodMappingsBuilder<ROOT : Any>(override val json: JsonLibAutoMarshallingJson<ROOT>) :
        ManualMethodMappingsBuilder<ROOT, ROOT>(json) {

    inline fun <reified IN : Any, OUT : Any> handler(paramsFieldNames: Set<String>,
                                                     noinline block: (IN) -> OUT): RequestHandler<ROOT, ROOT> =
            handler(paramsFieldNames,
                    Params { json.asA(it, IN::class) },
                    Result { json.asJsonObject(it) },
                    block)

    inline fun <reified IN : Any, OUT : Any> handler(noinline block: (IN) -> OUT): RequestHandler<ROOT, ROOT> =
            handler(IN::class.javaObjectType.declaredFields.map { it.name }.toSet(), block)

    fun <OUT : Any> handler(block: () -> OUT): RequestHandler<ROOT, ROOT> =
            handler(Result { json.asJsonObject(it) }, block)
}