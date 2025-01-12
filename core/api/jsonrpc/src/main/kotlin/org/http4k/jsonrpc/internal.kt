package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType.Array
import org.http4k.lens.JsonRpcMapping

internal class ParamMappingJsonRequestHandler<NODE : Any, IN, OUT : Any>(
    json: Json<NODE>,
    paramsFieldNames: Iterable<String>,
    paramsLens: JsonRpcMapping<NODE, IN>,
    function: (IN) -> OUT,
    resultLens: JsonRpcMapping<OUT, NODE>
) : JsonRpcHandler<NODE, NODE> {

    private val handler = { it: NODE ->
        val input = when (json.typeOf(it)) {
            Array -> {
                val elements = json.elements(it).toList()
                paramsFieldNames.mapIndexed { index: Int, name: String ->
                    name to elements.getOrElse(index) { json.nullNode() }
                }.takeUnless { it.isEmpty() }
                    ?.let { json.obj(it) }
                    ?: json.nullNode()
            }

            else -> it
        }
        paramsLens(input).let(function).let(resultLens)
    }

    override fun invoke(request: NODE): NODE = handler(request)
}

internal class NoParamsJsonRequestHandler<NODE, OUT : Any>(function: () -> OUT, resultLens: JsonRpcMapping<OUT, NODE>) :
    JsonRpcHandler<NODE, NODE> {

    private val handler: (NODE) -> NODE = { function().let(resultLens) }

    override fun invoke(request: NODE): NODE = handler(request)
}
