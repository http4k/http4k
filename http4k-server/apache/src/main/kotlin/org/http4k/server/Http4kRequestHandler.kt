package org.http4k.server

import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.EndpointDetails
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpEntityContainer
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.io.HttpRequestHandler
import org.apache.hc.core5.http.io.entity.EmptyInputStream
import org.apache.hc.core5.http.io.entity.InputStreamEntity
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.http.protocol.HttpCoreContext
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.safeLong
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import java.net.InetSocketAddress
import java.net.URI

/**
 * Exposed to allow for insertion into a customised Apache WebServer instance
 */
class Http4kRequestHandler(handler: HttpHandler) : HttpRequestHandler {

    private val safeHandler = ServerFilters.CatchAll().then(handler)

    override fun handle(request: ClassicHttpRequest, response: ClassicHttpResponse, context: HttpContext) =
        (request.asHttp4kRequest(context)?.let(safeHandler) ?: Response(NOT_IMPLEMENTED)).into(response)

    private fun HttpRequest.asHttp4kRequest(context: HttpContext): Request? {
        val connection = context.getAttribute(HttpCoreContext.CONNECTION_ENDPOINT) as EndpointDetails
        return Method.supportedOrNull(method)?.let {
            Request(it, uri.httpUri())
                .headers(headers.toHttp4kHeaders()).let {
                    when (this) {
                        is HttpEntityContainer -> entity?.let { httpEntity ->
                            it.body(
                                httpEntity.content,
                                getFirstHeader("Content-Length")?.value.safeLong()
                            )
                        } ?: it

                        else -> it.body(EmptyInputStream.INSTANCE, 0)
                    }
                }
                .source((connection.remoteAddress as InetSocketAddress).let {
                    RequestSource(
                        it.hostString,
                        it.port,
                        uri.scheme
                    )
                })
        }
    }

    private fun URI.httpUri(): String = path + if (query.isNullOrBlank()) "" else "?$query"

    private val headersThatApacheInterceptorSets = setOf("Transfer-Encoding", "Content-Length")

    private fun Response.into(response: HttpResponse) {
        response.code = status.code
        response.reasonPhrase = status.description
        headers.filter { !headersThatApacheInterceptorSets.contains(it.first) }
            .forEach { (key, value) -> response.addHeader(key, value) }
        if (response is HttpEntityContainer) {
            val contentType = org.http4k.lens.Header.CONTENT_TYPE(this@into)
                ?.let { ContentType.parse(it.toHeaderValue()) }
                ?: ContentType.WILDCARD
            response.entity = InputStreamEntity(body.stream, body.length ?: -1L, contentType)
        }
    }

    private fun Array<Header>.toHttp4kHeaders(): Headers = map { it.name to it.value }
}
