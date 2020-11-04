@file:Suppress("unused")

package org.http4k.serverless

import com.qcloud.scf.runtime.Context
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.InitialiseRequestContext

const val TENCENT_REQUEST_KEY = "HTTP4K_TENCENT_REQUEST"
const val TENCENT_CONTEXT_KEY = "HTTP4K_TENCENT_CONTEXT"

abstract class TencentCloudFunction(appLoader: AppLoaderWithContexts) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    fun handleRequest(request: APIGatewayProxyRequestEvent, context: Context?) =
        InitialiseRequestContext(contexts)
            .then(AddTencent(request, context, contexts))
            .then(app)(request.asHttp4kRequest())
            .asTencent()
}

private fun AddTencent(request: APIGatewayProxyRequestEvent, ctx: Context?, contexts: RequestContexts) = Filter { next ->
    {
        ctx?.apply { contexts[it][TENCENT_CONTEXT_KEY] = this }
        contexts[it][TENCENT_REQUEST_KEY] = request
        next(it)
    }
}

private fun APIGatewayProxyRequestEvent.asHttp4kRequest(): Request {
    val withHeaders = (headers ?: emptyMap()).toList().fold(
        Request(Method.valueOf(httpMethod), buildUri())
            .body(body?.let(::MemoryBody) ?: Body.EMPTY)) { memo, (first, second) ->
        memo.header(first, second)
    }

    return queryStringParameters.entries.fold(withHeaders, { acc, (first, second) -> acc.query(first, second) })
}

private fun APIGatewayProxyRequestEvent.buildUri() =
    Uri.of(path)

private fun Response.asTencent() = APIGatewayProxyResponseEvent().also {
    it.statusCode = status.code
    it.headers = headers.toMap()
    it.body = bodyString()
}
