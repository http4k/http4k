package org.http4k.serverless.gcp

import com.google.cloud.functions.HttpFunction
import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import org.http4k.core.*
import java.util.stream.Collectors

open class Http4kGCFAdapter(private val handler: HttpHandler) : HttpFunction {

    private fun HttpRequest.asHttp4kRequest(): Request =
        Request(Method.valueOf(method), uri)
            .headers(toHttp4kHeaders(headers)).body(reader.lines().collect(Collectors.joining()))

    private fun Response.into(response: HttpResponse) {
        response.setStatusCode(status.code, status.description)
        headers.forEach { (key, value) -> response.appendHeader(key, value) }
        body.stream.use { input -> response.outputStream.use { output -> input.copyTo(output) } }
    }

    private fun toHttp4kHeaders(gcpHeaders: Map<String, List<String>>): Headers = gcpHeaders.entries.map { gcpHeader ->
        gcpHeader.value.map { Pair(gcpHeader.key, it) }
    }.flatten()

    override fun service(request: HttpRequest, response: HttpResponse) {
        handler(request.asHttp4kRequest()).into(response)
    }
}
