package org.http4k.serverless

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.InitialiseRequestContext

const val GCF_REQUEST_KEY = "HTTP4K_GCF_REQUEST"

open class GoogleCloudFunction(appLoader: AppLoaderWithContexts) : HttpFunction {
    constructor(input: AppLoader) : this(AppLoaderWithContexts { env, _ -> input(env) })
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val contexts = RequestContexts()
    private val app = appLoader(System.getenv(), contexts)

    override fun service(request: HttpRequest, response: HttpResponse) =
        CatchAll()
            .then(InitialiseRequestContext(contexts))
            .then(AddGCPRequest(request, contexts))
            .then(app)(request.asHttp4kRequest())
            .into(response)
}

private fun HttpRequest.asHttp4kRequest() = Request(valueOf(method), uri).headers(toHttp4kHeaders(headers)).body(inputStream)

private fun Response.into(response: HttpResponse) {
    response.setStatusCode(status.code, status.description)
    headers.forEach { (key, value) -> response.appendHeader(key, value) }
    body.stream.use { input -> response.outputStream.use { output -> input.copyTo(output) } }
}

private fun toHttp4kHeaders(gcfHeaders: Map<String, List<String>>) = gcfHeaders.entries
    .map { gcfHeader ->
        gcfHeader.value.map { Pair(gcfHeader.key, it) }
    }.flatten()

private fun AddGCPRequest(request: HttpRequest, contexts: RequestContexts) = Filter { next ->
    {
        contexts[it][GCF_REQUEST_KEY] = request
        next(it)
    }
}
