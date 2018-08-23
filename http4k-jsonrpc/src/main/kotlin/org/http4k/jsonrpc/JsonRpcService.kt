package org.http4k.jsonrpc

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.LensFailure

data class JsonRpcService<ROOT : NODE, NODE>(private val json: Json<ROOT, NODE>,
                                             private val errorHandler: ErrorHandler,
                                             private val methodMappings: List<MethodMapping<NODE, NODE>>): HttpHandler {

    private val jsonLens = json.body("JSON-RPC request", ContentNegotiation.StrictNoDirective).toLens()
    private val methods = methodMappings.map { it.name to it.handler }.toMap()

    private val handler: HttpHandler = ServerFilters.CatchLensFailure {
        Response(Status.OK).with(jsonLens of renderError(ErrorMessage.ParseError))
    }.then { request ->
        if (request.method == Method.POST) {
            val responseJson = process(jsonLens(request))
            if (responseJson == null) {
                Response(Status.NO_CONTENT).with(Header.Common.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            } else {
                Response(Status.OK).with(jsonLens of responseJson)
            }
        } else {
            Response(Status.METHOD_NOT_ALLOWED)
        }
    }

    override fun invoke(request: Request): Response = handler(request)

    private fun process(requestJson: ROOT): ROOT? = when (json.typeOf(requestJson)) {
        JsonType.Object -> processSingleRequest(json.fields(requestJson).toMap())
        JsonType.Array -> handleBatchRequest(json.elements(requestJson).toList())
        else -> renderError(ErrorMessage.InvalidRequest)
    }

    private fun processSingleRequest(fields: Map<String, NODE>): ROOT? {
        val request = JsonRpcRequest(json, fields)
        return try {
            if (request.valid()) {
                val method = methods[request.method]
                if (method == null) {
                    renderError(ErrorMessage.MethodNotFound, request.id)
                } else {
                    val result = method(request.params ?: json.nullNode())
                    request.id?.let { renderResult(result, it) }
                }
            } else {
                renderError(ErrorMessage.InvalidRequest, request.id)
            }
        } catch (e: Throwable) {
            val defaultErrorMessage = if (e is LensFailure && e.overall() == Failure.Type.Invalid) {
                ErrorMessage.InvalidParams
            } else {
                ErrorMessage.ServerError
            }
            val error = when (e) {
                is LensFailure -> e.cause ?: e
                else -> e
            }
            renderError(errorHandler(error) ?: defaultErrorMessage, request.id)
        }
    }

    private fun handleBatchRequest(elements: List<NODE>): ROOT? {
        return if (elements.isNotEmpty()) {
            val batchResults = mutableListOf<ROOT>()
            elements.forEach {
                processSingleRequest(json.fields(it).toMap())?.also {
                    batchResults.add(it)
                }
            }
            batchResults.takeIf { it.isNotEmpty() }?.let { json.array(it) }
        } else {
            renderError(ErrorMessage.InvalidRequest)
        }
    }

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

data class MethodMapping<IN, OUT>(val name: String, val handler: RequestHandler<IN, OUT>)

typealias ErrorHandler = (Throwable) -> ErrorMessage?

private const val jsonRpcVersion: String = "2.0"

private class JsonRpcRequest<ROOT: NODE, NODE>(json: Json<ROOT, NODE>, fields: Map<String, NODE>) {
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
        if (!setOf(JsonType.Object, JsonType.Array).contains(json.typeOf(it))) {
            valid = false
        }
    }

    val id: NODE? = fields["id"]?.let {
        if (!setOf(JsonType.String, JsonType.Number, JsonType.Null).contains(json.typeOf(it))) {
            valid = false
            json.nullNode()
        } else {
            it
        }
    }

    fun valid() = valid
}