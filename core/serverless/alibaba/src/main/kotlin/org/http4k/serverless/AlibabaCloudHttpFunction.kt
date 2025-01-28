package org.http4k.serverless

import com.aliyun.fc.runtime.Context
import com.aliyun.fc.runtime.HttpRequestHandler
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.lens.RequestKey
import org.http4k.servlet.asHttp4kRequest
import org.http4k.servlet.transferTo
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


val ALIBABA_REQUEST_KEY = RequestKey.of<HttpServletRequest>("HTTP4K_ALIBABA_REQUEST")
val ALIBABA_CONTEXT_KEY = RequestKey.of<Context>("HTTP4K_ALIBABA_CONTEXT")

abstract class AlibabaCloudHttpFunction(appLoader: AppLoader) : HttpRequestHandler {
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val app = appLoader(System.getenv())

    override fun handleRequest(request: HttpServletRequest, response: HttpServletResponse, context: Context?) {
        CatchAll()
            .then(AddAlibabaRequest(request, context))
            .then(app)(request.asHttp4kRequest())
            .transferTo(response)
    }
}

private fun AddAlibabaRequest(request: HttpServletRequest, ctx: Context?) = Filter { next ->
    {
        val reqWithReq = it.with(ALIBABA_REQUEST_KEY of request)
        next(ctx?.run { reqWithReq.with(ALIBABA_CONTEXT_KEY of this) } ?: reqWithReq)
    }
}
