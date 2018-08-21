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
                                             private val methodMappings: List<MethodMapping<NODE, NODE>>): HttpHandler {

    private val jsonLens = json.body("JSON-RPC request", ContentNegotiation.StrictNoDirective).toLens()
    private val methods = methodMappings.map { it.name to it.handler }.toMap()

    private val handler: HttpHandler = ServerFilters.CatchLensFailure {
        Response(Status.OK).with(jsonLens of renderError(ErrorMessage.ParseError))
    }.then { request ->
        if (request.method == Method.POST) {
            val responseJson = processRequest(jsonLens(request))
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

    private fun processRequest(requestJson: ROOT): ROOT? = when (json.typeOf(requestJson)) {
        JsonType.Object -> handleSingle(json.fields(requestJson).toMap())
        JsonType.Array -> handleBatch(json.elements(requestJson).toList())
        else -> renderError(ErrorMessage.InvalidRequest)
    }

    private fun handleSingle(fields: Map<String, NODE>): ROOT? {
        val request = JsonRpcRequest(json, fields)
        return try {
            val result: NODE = if (request.valid()) {
                methods[request.method]?.invoke(request.params ?: json.nullNode())
                        ?: throw JsonRpcException(ErrorMessage.MethodNotFound)
            } else {
                throw JsonRpcException(ErrorMessage.InvalidRequest)
            }

            request.id?.let { renderResult(result, it) }
        } catch (e: Throwable) {
            when (e) {
                is JsonRpcException -> renderError(e.errorMessage, request.id)
                is LensFailure -> when (e.overall()) {
                    Failure.Type.Invalid -> renderError(ErrorMessage.InvalidParams, request.id)
                    else -> renderError(ErrorMessage.ServerError, request.id)
                }
                else -> renderError(ErrorMessage.ServerError, request.id)
            }
        }
    }

    private fun handleBatch(elements: List<NODE>): ROOT? {
        return if (elements.isNotEmpty()) {
            val batchResults = mutableListOf<ROOT>()
            elements.forEach {
                handleSingle(json.fields(it).toMap())?.also {
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
            "error" to json.obj(
                    "code" to json.number(errorMessage.code),
                    "message" to json.string(errorMessage.message)
            ),
            "id" to (id ?: json.nullNode())
    )
}

data class MethodMapping<IN, OUT>(val name: String, val handler: RequestHandler<IN, OUT>)
