package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType

internal class ParamMappingJsonRequestHandler<NODE : Any, IN, OUT : Any>(
    json: Json<NODE>,
    paramsFieldNames: Iterable<String>,
    paramsLens: Mapping<NODE, IN>,
    function: (IN) -> OUT,
    resultLens: Mapping<OUT, NODE>
) : JsonRpcHandler<NODE, NODE> {
    private val handler: (NODE) -> NODE = {
        val input = when (json.typeOf(it)) {
            JsonType.Array -> {
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

internal class NoParamsJsonRequestHandler<NODE, OUT : Any>(function: () -> OUT, resultLens: Mapping<OUT, NODE>) :
    JsonRpcHandler<NODE, NODE> {

    private val handler: (NODE) -> NODE = { function().let(resultLens) }

    override fun invoke(request: NODE): NODE = handler(request)
}
