package org.http4k.serverless

import com.aliyun.fc.runtime.Context
import com.aliyun.fc.runtime.HttpRequestHandler
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.RequestContexts
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.servlet.asHttp4kRequest
import org.http4k.servlet.transferTo
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

const val ALIBABA_REQUEST_KEY = "HTTP4K_ALIBABA_REQUEST"
const val ALIBABA_CONTEXT_KEY = "HTTP4K_ALIBABA_CONTEXT"

abstract class AlibabaCloudFunction(appLoader: AppLoaderWithContexts) : HttpRequestHandler {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    override fun handleRequest(request: HttpServletRequest, response: HttpServletResponse, context: Context?) {
        CatchAll()
            .then(InitialiseRequestContext(contexts))
            .then(AddAlibabaRequest(request, context, contexts))
            .then(app)(request.asHttp4kRequest())
            .transferTo(response)
    }
}

private fun AddAlibabaRequest(request: HttpServletRequest, ctx: Context?, contexts: RequestContexts) = Filter { next ->
    {
        ctx?.apply { contexts[it][ALIBABA_CONTEXT_KEY] = this }
        contexts[it][ALIBABA_REQUEST_KEY] = request
        next(it)
    }
}
