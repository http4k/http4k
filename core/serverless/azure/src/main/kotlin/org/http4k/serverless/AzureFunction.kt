package org.http4k.serverless

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import kotlinx.coroutines.runBlocking
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.lens.RequestKey
import java.util.Optional

val AZURE_REQUEST_KEY = RequestKey.required<HttpRequestMessage<Optional<String>>>("HTTP4K_AZURE_REQUEST")
val AZURE_CONTEXT_KEY = RequestKey.required<ExecutionContext>("HTTP4K_AZURE_CONTEXT")

abstract class AzureFunction(appLoader: AppLoader) {
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val app = runBlocking { appLoader(System.getenv()) }

    abstract fun handleRequest(
        req: HttpRequestMessage<Optional<String>>,
        ctx: ExecutionContext
    ): HttpResponseMessage

    protected fun handle(request: HttpRequestMessage<Optional<String>>, ctx: ExecutionContext) =
        runBlocking {
            CatchAll()
                .then(AddAzure(request, ctx))
                .then(app)(request.asHttp4k())
                .asAzure(request)
        }
}

private fun AddAzure(request: HttpRequestMessage<Optional<String>>, ctx: ExecutionContext) = Filter { next ->
    {
        next(it.with(AZURE_CONTEXT_KEY of ctx, AZURE_REQUEST_KEY of request))
    }
}

fun HttpRequestMessage<Optional<String>>.asHttp4k() = headers.entries.fold(
    Request(Method.valueOf(httpMethod.name), uri.toString()).body(this.body.orElse(""))
) { acc, next -> acc.header(next.key, next.value) }

fun Response.asAzure(request: HttpRequestMessage<Optional<String>>) =
    headers.fold(
        request.createResponseBuilder(HttpStatus.valueOf(status.code)).body(bodyString())
    ) { acc, next ->
        acc.header(next.first, next.second)
    }.build()
