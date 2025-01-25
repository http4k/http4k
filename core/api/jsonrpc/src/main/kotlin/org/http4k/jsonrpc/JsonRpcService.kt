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
import org.http4k.format.renderError
import org.http4k.jsonrpc.ErrorMessage.Companion.ParseError
import org.http4k.lens.ContentNegotiation.Companion.StrictNoDirective
import org.http4k.lens.Header.CONTENT_TYPE

data class JsonRpcService<NODE>(
    private val processor: RoutingJsonRpcHandler<NODE>,
    private val json: Json<NODE>,
) : HttpHandler {

    constructor(
        json: Json<NODE>,
        errorHandler: ErrorHandler,
        bindings: Iterable<JsonRpcMethodBinding<NODE, NODE>>
    ) : this(RoutingJsonRpcHandler(json, errorHandler, bindings), json)

    private val jsonLens = json.body("JSON-RPC request", StrictNoDirective).toLens()

    private val handler = CatchLensFailure { _ -> Response(OK).with(jsonLens of json.renderError(ParseError)) }
        .then(Filter { next -> { if (it.method == POST) next(it) else Response(METHOD_NOT_ALLOWED) } })
        .then {
            when (val responseJson = processor(jsonLens(it))) {
                null -> Response(NO_CONTENT).with(CONTENT_TYPE of APPLICATION_JSON)
                else -> Response(OK).with(jsonLens of responseJson)
            }
        }

    override fun invoke(request: Request): Response = handler(request)
}

const val jsonRpcVersion: String = "2.0"
