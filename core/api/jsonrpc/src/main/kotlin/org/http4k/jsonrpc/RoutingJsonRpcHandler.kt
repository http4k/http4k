package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType.Array
import org.http4k.format.JsonType.Object
import org.http4k.format.renderError
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.lens.Failure.Type.Invalid
import org.http4k.lens.LensFailure

class RoutingJsonRpcHandler<NODE>(
    private val json: Json<NODE>,
    private val errorHandler: ErrorHandler,
    bindings: Iterable<JsonRpcMethodBinding<NODE, NODE>>
) : JsonRpcHandler<NODE, NODE?> {

    private val methods = bindings.associate { it.name to it.handler }

    override operator fun invoke(requestJson: NODE): NODE? = when (json.typeOf(requestJson)) {
        Object -> processSingleRequest(json.fields(requestJson).toMap())
        Array -> processBatchRequest(json.elements(requestJson).toList())
        else -> json.renderError(InvalidRequest)
    }

    private fun processSingleRequest(fields: Map<String, NODE>) =
        JsonRpcRequest(json, fields).mapIfValid { request ->
            try {
                when (val method = methods[request.method]) {
                    null -> json.renderError(ErrorMessage.MethodNotFound, request.id)
                    else -> with(method(request.params ?: json.nullNode())) {
                        request.id?.let { json.renderResult(this, it) }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is LensFailure -> {
                        val errorMessage = errorHandler(e.cause ?: e)
                            ?: if (e.overall() == Invalid) InvalidParams else InternalError
                        json.renderError(errorMessage, request.id)
                    }

                    else -> json.renderError(errorHandler(e) ?: InternalError, request.id)
                }
            }
        }

    private fun JsonRpcRequest<NODE>.mapIfValid(block: (JsonRpcRequest<NODE>) -> NODE?) = when {
        valid() -> block(this)
        else -> json.renderError(InvalidRequest, id)
    }

    private fun processBatchRequest(elements: List<NODE>) = with(elements) {
        if (isNotEmpty()) processEachAsSingleRequest() else json.renderError(InvalidRequest)
    }

    private fun List<NODE>.processEachAsSingleRequest() = json {
        mapNotNull {
            processSingleRequest(if (typeOf(it) == Object) fields(it).toMap() else emptyMap())
        }.takeIf { it.isNotEmpty() }?.let { array(it) }
    }
}
