package org.http4k.server

import org.apache.http.Header
import org.apache.http.HttpEntityEnclosingRequest
import org.apache.http.HttpInetConnection
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.io.EmptyInputStream
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpCoreContext
import org.apache.http.protocol.HttpRequestHandler
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.filter.ServerFilters

/**
 * Exposed to allow for insertion into a customised Apache WebServer instance
 */
class Http4kApache4RequestHandler(handler: HttpHandler) : HttpRequestHandler {

    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun handle(request: HttpRequest, response: HttpResponse, context: HttpContext) =
        (request.asHttp4kRequest(context)?.let(safeHandler) ?: Response(Status.NOT_IMPLEMENTED)).into(response)

    private fun HttpRequest.asHttp4kRequest(context: HttpContext): Request? {
        val connection = context.getAttribute(HttpCoreContext.HTTP_CONNECTION) as HttpInetConnection
        return Method.supportedOrNull(requestLine.method)?.let {
            Request(it, requestLine.uri, protocolVersion.toString())
                .headers(allHeaders.toHttp4kHeaders()).let {
                    when (this) {
                        is HttpEntityEnclosingRequest -> it.body(
                            entity.content,
                            getFirstHeader("Content-Length")?.value.safeLong()
                        )

                        else -> it.body(EmptyInputStream.INSTANCE, 0)
                    }
                }
                .source(RequestSource(connection.remoteAddress.hostAddress, connection.remotePort))
        }
    }

    private val headersThatApacheInterceptorSets = setOf("Transfer-Encoding", "Content-Length")

    private fun Response.into(response: HttpResponse) {
        with(response) {
            setStatusCode(status.code)
            setReasonPhrase(status.description)
            headers.filter { !headersThatApacheInterceptorSets.contains(it.first) }
                .forEach { (key, value) -> addHeader(key, value) }
            entity = InputStreamEntity(body.stream, body.length ?: -1L)
        }
    }

    private fun Array<Header>.toHttp4kHeaders(): Headers = map { it.name to it.value }
}
