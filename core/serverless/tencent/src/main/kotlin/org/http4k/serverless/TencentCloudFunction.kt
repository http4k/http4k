@file:Suppress("unused")

package org.http4k.serverless

import com.alibaba.fastjson.JSONObject
import com.qcloud.scf.runtime.Context
import com.qcloud.services.scf.runtime.events.APIGatewayProxyRequestEvent
import com.qcloud.services.scf.runtime.events.APIGatewayProxyResponseEvent
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.lens.RequestKey
import org.http4k.server.supportedOrNull

val TENCENT_REQUEST_KEY = RequestKey.required<APIGatewayProxyRequestEvent>("HTTP4K_TENCENT_REQUEST")
val TENCENT_CONTEXT_KEY = RequestKey.required<Context>("HTTP4K_TENCENT_CONTEXT")

abstract class TencentCloudFunction(appLoader: AppLoader) {
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val app = appLoader(System.getenv())

    fun handleRequest(request: APIGatewayProxyRequestEvent, context: Context?) =
        (request.asHttp4kRequest()?.let {
            CatchAll()
                .then(AddTencent(request, context))
                .then(app)(it)
        } ?: Response(NOT_IMPLEMENTED)).asTencent()
}

private fun AddTencent(request: APIGatewayProxyRequestEvent, ctx: Context?) =
    Filter { next ->
        {
            val reqWithReq = it.with(TENCENT_REQUEST_KEY of request)
            next(ctx?.run { reqWithReq.with(TENCENT_CONTEXT_KEY of this) } ?: reqWithReq)
        }
    }

private fun APIGatewayProxyRequestEvent.asHttp4kRequest(): Request? =
    Method.supportedOrNull(httpMethod)?.let { method ->
        val withHeaders = (headers ?: emptyMap()).toList().fold(
            Request(method, buildUri()).body(body?.let(::MemoryBody) ?: Body.EMPTY)
        ) { memo, (first, second) -> memo.header(first, second) }

        queryStringParameters.entries.fold(withHeaders) { acc, (first, second) -> acc.query(first, second) }
    }

private fun APIGatewayProxyRequestEvent.buildUri() =
    Uri.of(path)

private fun Response.asTencent() = APIGatewayProxyResponseEvent().also {
    it.statusCode = status.code
    it.headers = JSONObject(headers.toMap())
    it.body = bodyString()
}
