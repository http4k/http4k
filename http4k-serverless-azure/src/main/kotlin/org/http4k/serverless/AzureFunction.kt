package org.http4k.serverless

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import java.util.Optional

const val AZURE_REQUEST_KEY = "HTTP4K_AZURE_REQUEST"
const val AZURE_CONTEXT_KEY = "HTTP4K_AZURE_CONTEXT"

abstract class AzureFunction(appLoader: AppLoaderWithContexts) {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    abstract fun handleRequest(req: HttpRequestMessage<Optional<String>>,
                               ctx: ExecutionContext): HttpResponseMessage

    protected fun handle(request: HttpRequestMessage<Optional<String>>, ctx: ExecutionContext) =
        InitialiseRequestContext(contexts)
            .then(AddAzure(request, ctx, contexts))
            .then(app)(request.asHttp4k())
            .asAzure(request)
}

private fun AddAzure(request: HttpRequestMessage<Optional<String>>, ctx: ExecutionContext,
                     contexts: RequestContexts) = Filter { next ->
    {
        contexts[it][AZURE_CONTEXT_KEY] = ctx
        contexts[it][AZURE_REQUEST_KEY] = request
        next(it)
    }
}

fun HttpRequestMessage<Optional<String>>.asHttp4k(): Request {
    println("URI: $uri")
    return headers.entries.fold(
        Request(Method.valueOf(httpMethod.name), uri.toString()).body(this.body.orElse(""))
    ) { acc, next -> acc.header(next.key, next.value) }
}

fun Response.asAzure(request: HttpRequestMessage<Optional<String>>) =
    headers.fold(
        request.createResponseBuilder(HttpStatus.valueOf(status.code)).body(bodyString())
    ) { acc, next ->
        acc.header(next.first, next.second)
    }.build()
