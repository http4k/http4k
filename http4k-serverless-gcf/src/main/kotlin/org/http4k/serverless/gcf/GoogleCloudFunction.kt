package org.http4k.serverless.gcf

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response

open class GoogleCloudFunction(private val handler: HttpHandler) : HttpFunction {

    override fun service(request: HttpRequest, response: HttpResponse) = handler(request.asHttp4kRequest()).into(response)

    private fun HttpRequest.asHttp4kRequest(): Request =
        Request(Method.valueOf(method), uri).headers(toHttp4kHeaders(headers)).body(inputStream)

    private fun Response.into(response: HttpResponse) {
        response.setStatusCode(status.code, status.description)
        headers.forEach { (key, value) -> response.appendHeader(key, value) }
        body.stream.use { input -> response.outputStream.use { output -> input.copyTo(output) } }
    }

    private fun toHttp4kHeaders(gcfHeaders: Map<String, List<String>>): Headers = gcfHeaders.entries.map { gcfHeader ->
        gcfHeader.value.map { Pair(gcfHeader.key, it) }
    }.flatten()
}
