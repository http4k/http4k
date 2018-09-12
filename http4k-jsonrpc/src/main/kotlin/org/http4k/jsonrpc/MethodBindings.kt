package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson

interface MethodBindings<ROOT : NODE, NODE> : Iterable<JsonRpcMethodBinding<NODE, NODE>> {
    fun method(name: String, handler: JsonRpcHandler<NODE, NODE>)
    val json: Json<ROOT, NODE>

    companion object {
        open class Manual<ROOT : NODE, NODE : Any>(override val json: Json<ROOT, NODE>) :
                MethodBindings<ROOT, NODE> {
            override fun iterator(): Iterator<JsonRpcMethodBinding<NODE, NODE>> = methodMappings
                    .map { JsonRpcMethodBinding(it.key, it.value) }.iterator()

            private val methodMappings = mutableMapOf<String, JsonRpcHandler<NODE, NODE>>()

            override fun method(name: String, handler: JsonRpcHandler<NODE, NODE>) {
                methodMappings[name] = handler
            }

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

        class Auto<ROOT : Any>(override val json: JsonLibAutoMarshallingJson<ROOT>) :
                Manual<ROOT, ROOT>(json) {

            inline fun <reified IN : Any, OUT : Any> handler(paramsFieldNames: Set<String>,
                                                             noinline fn: (IN) -> OUT): JsonRpcHandler<ROOT, ROOT> =
                    handler(paramsFieldNames, Params { json.asA(it, IN::class) }, Result { json.asJsonObject(it) }, fn)

            inline fun <reified IN : Any, OUT : Any> handler(noinline block: (IN) -> OUT): JsonRpcHandler<ROOT, ROOT> =
                    handler(IN::class.javaObjectType.declaredFields.map { it.name }.toSet(), block)

            fun <OUT : Any> handler(block: () -> OUT): JsonRpcHandler<ROOT, ROOT> =
                    handler(Result { json.asJsonObject(it) }, block)
        }
    }
}