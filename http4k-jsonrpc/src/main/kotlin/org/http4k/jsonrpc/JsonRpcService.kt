package org.http4k.jsonrpc

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
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
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.LensFailure

data class JsonRpcService<ROOT : NODE, NODE : Any>(
        private val json: Json<ROOT, NODE>,
        private val errorHandler: ErrorHandler,
        private val methodMappings: List<MethodMapping<NODE, NODE>>) : HttpHandler {

    private val jsonLens = json.body("JSON-RPC request", StrictNoDirective).toLens()
    private val methods = methodMappings.map { it.name to it.handler }.toMap()

    private val handler: HttpHandler = ServerFilters.CatchLensFailure {
        Response(OK).with(jsonLens of renderError(ParseError))
    }.then { request ->
        when (request.method) {
            POST -> {
                val responseJson = process(jsonLens(request))
                when (responseJson) {
                    null -> Response(NO_CONTENT).with(CONTENT_TYPE of APPLICATION_JSON)
                    else -> Response(OK).with(jsonLens of responseJson)
                }
            }
            else -> Response(METHOD_NOT_ALLOWED)
        }
    }

    override fun invoke(request: Request): Response = handler(request)

    private fun process(requestJson: ROOT): ROOT? = when (json.typeOf(requestJson)) {
        Object -> processSingleRequest(json.fields(requestJson).toMap())
        Array -> processBatchRequest(json.elements(requestJson).toList())
        else -> renderError(InvalidRequest)
    }

    private fun processSingleRequest(fields: Map<String, NODE>): ROOT? =
            JsonRpcRequest(json, fields).mapIfValid { request ->
                try {
                    val method = methods[request.method]
                    if (method == null) {
                        renderError(MethodNotFound, request.id)
                    } else {
                        val result = method(request.params ?: json.nullNode())
                        request.id?.let { renderResult(result, it) }
                    }
                } catch (e: Throwable) {
                    val error = when (e) {
                        is LensFailure -> e.cause ?: e
                        else -> e
                    }
                    renderError(errorHandler(error) ?: e.defaultErrorMessage(), request.id)
                }
            }

    private fun JsonRpcRequest<ROOT, NODE>.mapIfValid(block: (JsonRpcRequest<ROOT, NODE>) -> ROOT?): ROOT? = when {
        valid() -> block(this)
        else -> renderError(InvalidRequest, id)
    }

    private fun Throwable.defaultErrorMessage(): ErrorMessage = when {
        this is LensFailure && overall() == Invalid -> InvalidParams
        else -> InternalError
    }

    private fun processBatchRequest(elements: List<NODE>): ROOT? = when {
        elements.isNotEmpty() -> elements.processEachAsSingleRequest()
        else -> renderError(InvalidRequest)
    }

    private fun List<NODE>.processEachAsSingleRequest(): ROOT? =
            mapNotNull { element ->
                processSingleRequest(json.fields(element).toMap())
            }.takeIf { it.isNotEmpty() }?.let { json.array(it) }

    private fun renderResult(result: NODE, id: NODE): ROOT = json.obj(
            "jsonrpc" to json.string(jsonRpcVersion),
            "result" to result,
            "id" to id
    )

    private fun renderError(errorMessage: ErrorMessage, id: NODE? = null): ROOT = json.obj(
            "jsonrpc" to json.string(jsonRpcVersion),
            "error" to errorMessage(json),
            "id" to (id ?: json.nullNode())
    )
}

data class MethodMapping<IN, OUT>(val name: String, val handler: JsonRpcHandler<IN, OUT>)

typealias ErrorHandler = (Throwable) -> ErrorMessage?

private const val jsonRpcVersion: String = "2.0"

private class JsonRpcRequest<ROOT : NODE, NODE>(json: Json<ROOT, NODE>, fields: Map<String, NODE>) {
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