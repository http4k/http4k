package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson

interface MethodBindings<NODE> : Iterable<JsonRpcMethodBinding<NODE, NODE>> {
    fun method(name: String, handler: JsonRpcHandler<NODE, NODE>)

    companion object {
        open class Manual<NODE : Any>(private val json: Json<NODE>) :
            MethodBindings<NODE> {
            override fun iterator() = methodMappings
                .map { JsonRpcMethodBinding(it.key, it.value) }.iterator()

            private val methodMappings = mutableMapOf<String, JsonRpcHandler<NODE, NODE>>()

            override fun method(name: String, handler: JsonRpcHandler<NODE, NODE>) {
                methodMappings[name] = handler
            }

            fun <IN, OUT : Any> handler(
                paramsLens: Mapping<NODE, IN>,
                resultLens: Mapping<OUT, NODE>,
                fn: (IN) -> OUT
            ): JsonRpcHandler<NODE, NODE> =
                handler(emptySet(), paramsLens, resultLens, fn)

            fun <IN, OUT : Any> handler(
                paramsFieldNames: Set<String>,
                paramsLens: Mapping<NODE, IN>,
                resultLens: Mapping<OUT, NODE>,
                fn: (IN) -> OUT
            ): JsonRpcHandler<NODE, NODE> =
                ParamMappingJsonRequestHandler(json, paramsFieldNames, paramsLens, fn, resultLens)

            fun <OUT : Any> handler(resultLens: Mapping<OUT, NODE>, block: () -> OUT): JsonRpcHandler<NODE, NODE> =
                NoParamsJsonRequestHandler(block, resultLens)
        }

        class Auto<NODE : Any>(val json: JsonLibAutoMarshallingJson<NODE>) :
            Manual<NODE>(json) {

            inline fun <reified IN : Any, OUT : Any> handler(
                paramsFieldNames: Set<String>,
                noinline fn: (IN) -> OUT
            ): JsonRpcHandler<NODE, NODE> =
                handler(paramsFieldNames, Mapping { json.asA(it, IN::class) }, Mapping { json.asJsonObject(it) }, fn)

            inline fun <reified IN : Any, OUT : Any> handler(noinline fn: (IN) -> OUT): JsonRpcHandler<NODE, NODE> =
                handler(IN::class.javaObjectType.declaredFields.map { it.name }.toSet(), fn)

            fun <OUT : Any> handler(fn: () -> OUT): JsonRpcHandler<NODE, NODE> =
                handler(Mapping { json.asJsonObject(it) }, fn)
        }
    }
}
