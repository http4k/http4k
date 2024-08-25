package org.http4k.server

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.io.Content
import org.eclipse.jetty.server.Handler.Abstract
import org.eclipse.jetty.util.Callback
import org.eclipse.jetty.util.thread.Invocable
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.safeLong
import org.eclipse.jetty.server.Request as JettyRequest
import org.eclipse.jetty.server.Response as JettyResponse

class Http4kJettyHttpHandler(private val handler: HttpHandler) : Abstract(Invocable.InvocationType.BLOCKING) {
    override fun handle(request: JettyRequest, response: JettyResponse, callback: Callback): Boolean {
        (request.asHttp4kRequest()?.let(handler) ?: Response(Status.NOT_IMPLEMENTED)).transferTo(response)
        callback.succeeded()
        return true
    }
}

internal fun JettyRequest.asHttp4kRequest(): Request? =
    Method.supportedOrNull(method)?.let {
        Request(it, Uri.of("${httpURI.pathQuery.orEmpty()}#${httpURI.fragment.orEmpty()}"), connectionMetaData.httpVersion.asString())
            .headers(headerParameters())
            .source(RequestSource(JettyRequest.getRemoteAddr(this), JettyRequest.getRemotePort(this), httpURI.scheme))
            .body(Content.Source.asInputStream(this), headers[HttpHeader.CONTENT_LENGTH].safeLong())
    }

private fun JettyRequest.headerParameters() =
    headers.fieldNamesCollection.map { name ->
        headers.getCSV(name, true).map { name to it }
    }.flatten()

private fun Response.transferTo(response: JettyResponse) {
    response.status = status.code
    headers.forEach { (key, value) -> if (value != null) response.headers.add(key, value) }
    body.stream.use { input -> Content.Sink.asOutputStream(response).use { output -> input.copyTo(output) } }
}
