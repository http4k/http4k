package org.http4k.jsonrpc

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.format.JsonType.Array
import org.http4k.format.JsonType.Object
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.ErrorMessage.Companion.ParseError
import org.http4k.lens.ContentNegotiation.Companion.StrictNoDirective
import org.http4k.lens.Failure.Type.Invalid
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.LensFailure

data class JsonRpcService<NODE : Any>(
    private val json: Json<NODE>,
    private val errorHandler: ErrorHandler,
    private val bindings: Iterable<JsonRpcMethodBinding<NODE, NODE>>) : HttpHandler {

    private val jsonLens = json.body("JSON-RPC request", StrictNoDirective).toLens()
    private val methods = bindings.map { it.name to it.handler }.toMap()

    private val handler = CatchLensFailure { Response(OK).with(jsonLens of renderError(ParseError)) }
        .then(Filter { next -> { if (it.method == POST) next(it) else Response(METHOD_NOT_ALLOWED) } })
        .then {
            when (val responseJson = process(jsonLens(it))) {
                null -> Response(NO_CONTENT).with(CONTENT_TYPE of APPLICATION_JSON)
                else -> Response(OK).with(jsonLens of responseJson)
            }
        }

    override fun invoke(request: Request): Response = handler(request)

    private fun process(requestJson: NODE): NODE? = when (json.typeOf(requestJson)) {
        Object -> processSingleRequest(json.fields(requestJson).toMap())
        Array -> processBatchRequest(json.elements(requestJson).toList())
        else -> renderError(InvalidRequest)
    }

    private fun processSingleRequest(fields: Map<String, NODE>) =
        JsonRpcRequest(json, fields).mapIfValid { request ->
            try {
                when (val method = methods[request.method]) {
                    null -> renderError(MethodNotFound, request.id)
                    else -> with(method(request.params ?: json.nullNode())) {
                        request.id?.let { renderResult(this, it) }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is LensFailure -> {
                        val errorMessage = errorHandler(e.cause ?: e)
                            ?: if (e.overall() == Invalid) InvalidParams else InternalError
                        renderError(errorMessage, request.id)
                    }
                    else -> renderError(errorHandler(e) ?: InternalError, request.id)
                }
            }
        }

    private fun JsonRpcRequest<NODE>.mapIfValid(block: (JsonRpcRequest<NODE>) -> NODE?) = when {
        valid() -> block(this)
        else -> renderError(InvalidRequest, id)
    }

    private fun processBatchRequest(elements: List<NODE>) = with(elements) {
        if (isNotEmpty()) processEachAsSingleRequest() else renderError(InvalidRequest)
    }

    private fun List<NODE>.processEachAsSingleRequest() = json {
        mapNotNull {
            processSingleRequest(if (typeOf(it) == Object) fields(it).toMap() else emptyMap())
        }.takeIf { it.isNotEmpty() }?.let { array(it) }
    }

    private fun renderResult(result: NODE, id: NODE): NODE = json {
        obj(
            "jsonrpc" to string(jsonRpcVersion),
            "result" to result,
            "id" to id
        )
    }

    private fun renderError(errorMessage: ErrorMessage, id: NODE? = null) = json {
        obj(
            "jsonrpc" to string(jsonRpcVersion),
            "error" to errorMessage(this),
            "id" to (id ?: nullNode())
        )
    }
}

data class JsonRpcMethodBinding<IN, OUT>(val name: String, val handler: JsonRpcHandler<IN, OUT>)

typealias ErrorHandler = (Throwable) -> ErrorMessage?

private const val jsonRpcVersion: String = "2.0"

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
        if (!setOf(JsonType.String, JsonType.Number, JsonType.Null).contains(json.typeOf(it))) {
            valid = false
            json.nullNode()
        } else it
    }

    fun valid() = valid
}
