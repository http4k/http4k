package org.http4k.jsonrpc

import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.format.JsonType.Array
import org.http4k.format.JsonType.Object
import org.http4k.lens.Failure
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
        else -> json.renderError(ErrorMessage.InvalidRequest)
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
                            ?: if (e.overall() == Failure.Type.Invalid) ErrorMessage.InvalidParams else ErrorMessage.InternalError
                        json.renderError(errorMessage, request.id)
                    }

                    else -> json.renderError(errorHandler(e) ?: ErrorMessage.InternalError, request.id)
                }
            }
        }

    private fun JsonRpcRequest<NODE>.mapIfValid(block: (JsonRpcRequest<NODE>) -> NODE?) = when {
        valid() -> block(this)
        else -> json.renderError(ErrorMessage.InvalidRequest, id)
    }

    private fun processBatchRequest(elements: List<NODE>) = with(elements) {
        if (isNotEmpty()) processEachAsSingleRequest() else json.renderError(ErrorMessage.InvalidRequest)
    }

    private fun List<NODE>.processEachAsSingleRequest() = json {
        mapNotNull {
            processSingleRequest(if (typeOf(it) == Object) fields(it).toMap() else emptyMap())
        }.takeIf { it.isNotEmpty() }?.let { array(it) }
    }
}

private class JsonRpcRequest<NODE>(json: Json<NODE>, fields: Map<String, NODE>) {
    private var valid = (fields["jsonrpc"] ?: json.nullNode()).let {
        json.typeOf(it) == JsonType.String && jsonRpcVersion == json.text(it)
    }

    val method: String = (fields["method"] ?: json.nullNode()).let {
        if (json.typeOf(it) == JsonType.String) {
            json.text(it)
        } else {
            valid = false
            ""
        }
    }
    val params: NODE? = fields["params"]?.also {
        if (!setOf(Object, Array).contains(json.typeOf(it))) valid = false
    }

    val id: NODE? = fields["id"]?.let {
        if (!setOf(JsonType.String, JsonType.Number, JsonType.Integer, JsonType.Null).contains(json.typeOf(it))) {
            valid = false
            json.nullNode()
        } else it
    }

    fun valid() = valid
}
