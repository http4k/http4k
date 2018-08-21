package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType

typealias RequestHandler<IN, OUT> = (IN) -> OUT

class ParamMappingJsonRequestHandler<ROOT: NODE, NODE, IN, OUT>(json: Json<ROOT, NODE>,
                                                                paramsFieldNames: Iterable<String>,
                                                                paramsLens: Params<NODE, IN>,
                                                                function: (IN) -> OUT, resultLens: Result<OUT, NODE>) :
        RequestHandler<NODE, NODE> {

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

class NoParamsJsonRequestHandler<NODE, OUT>(function: () -> OUT, resultLens: Result<OUT, NODE>) :
        RequestHandler<NODE, NODE> {

    private val handler: (NODE) -> NODE = { function().let(resultLens) }

    override fun invoke(request: NODE): NODE = handler(request)
}