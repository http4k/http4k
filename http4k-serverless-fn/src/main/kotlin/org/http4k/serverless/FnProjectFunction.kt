package org.http4k.serverless

import com.fnproject.fn.api.Headers
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import java.io.ByteArrayInputStream

const val FN_CONTEXT_KEY = "HTTP4K_FN_CONTEXT"

abstract class FnProjectFunction(appLoader: AppLoaderWithContexts) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    fun handleRequest(context: HTTPGatewayContext, body: ByteArray) =
        InitialiseRequestContext(contexts)
            .then(AddFnContext(context, contexts))
            .then(app)(asHttp4kRequest(context, body))
            .transferTo(context)
}

private fun AddFnContext(ctx: HTTPGatewayContext, contexts: RequestContexts) = Filter { next ->
    {
        contexts[it][FN_CONTEXT_KEY] = ctx
        next(it)
    }
}

private fun asHttp4kRequest(ctx: HTTPGatewayContext, body: ByteArray) =
    ctx.queryParameters.all.entries.fold(
        Request(Method.valueOf(ctx.method), ctx.requestURL)
            .headers(ctx.headers.asHttp4k()).body(ByteArrayInputStream(body))
    ) { acc, query ->
        query.value.fold(acc) { acc2, value -> acc2.query(query.key, value) }
    }

fun Headers.asHttp4k(): List<Pair<String, String?>> =
    asMap().entries.flatMap { it.value.map { v -> it.key to v } }

private fun Response.transferTo(ctx: HTTPGatewayContext): ByteArray {
    ctx.setStatusCode(status.code)
    headers
        .map { it.first to (it.second ?: "") }
        .groupBy { it.first }
        .forEach { it: Map.Entry<String, List<Pair<String, String>>> ->
            val values = it.value.map { it.second }
            ctx.setResponseHeader(it.key, values.first(), *values.drop(1).toTypedArray())
        }
    return body.payload.array()
}

