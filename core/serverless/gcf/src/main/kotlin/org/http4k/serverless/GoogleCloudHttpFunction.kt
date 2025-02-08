package org.http4k.serverless

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.valueOf
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.lens.RequestKey

val GCF_REQUEST_KEY = RequestKey.required<HttpRequest>("HTTP4K_GCF_REQUEST")

abstract class GoogleCloudHttpFunction(appLoader: AppLoader) : HttpFunction {
    constructor(input: HttpHandler) : this(AppLoader { input })

    private val app = appLoader(System.getenv())

    override fun service(request: HttpRequest, response: HttpResponse) =
        CatchAll()
            .then(AddGCPRequest(request))
            .then(app)(request.asHttp4kRequest())
            .into(response)
}

private fun HttpRequest.asHttp4kRequest() =
    Request(valueOf(method), uri).headers(toHttp4kHeaders(headers)).body(inputStream)

private fun Response.into(response: HttpResponse) {
    response.setStatusCode(status.code, status.description)
    headers.forEach { (key, value) -> response.appendHeader(key, value) }
    body.stream.use { input -> response.outputStream.use { output -> input.copyTo(output) } }
}

private fun toHttp4kHeaders(gcfHeaders: Map<String, List<String>>) = gcfHeaders.entries
    .map { gcfHeader -> gcfHeader.value.map { Pair(gcfHeader.key, it) } }
    .flatten()

private fun AddGCPRequest(request: HttpRequest) = Filter { next ->
    {
        next(it.with(GCF_REQUEST_KEY of request))
    }
}
