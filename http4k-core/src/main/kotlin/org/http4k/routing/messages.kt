package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import java.io.InputStream

interface RequestWithRoute : Request {
    val xUriTemplate: UriTemplate
}

interface ResponseWithRoute : Response {
    val xUriTemplate: UriTemplate
}

data class RoutedRequest(private val delegate: Request, override val xUriTemplate: UriTemplate) : Request by delegate, RequestWithRoute {
    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun method(method: Method): Request = RoutedRequest(delegate.method(method), xUriTemplate)

    override fun uri(uri: Uri): Request = RoutedRequest(delegate.uri(uri), xUriTemplate)

    override fun query(name: String, value: String?): Request = RoutedRequest(delegate.query(name, value), xUriTemplate)

    override fun removeQuery(name: String): Request = RoutedRequest(delegate.removeQuery(name), xUriTemplate)

    override fun removeQueries(prefix: String): Request = RoutedRequest(delegate.removeQueries(prefix), xUriTemplate)

    override fun source(source: RequestSource): Request = RoutedRequest(delegate.source(source), xUriTemplate)

    override fun header(name: String, value: String?): Request = RoutedRequest(delegate.header(name, value), xUriTemplate)

    override fun headers(headers: Headers): Request = RoutedRequest(delegate.headers(headers), xUriTemplate)

    override fun replaceHeader(name: String, value: String?): Request = RoutedRequest(delegate.replaceHeader(name, value), xUriTemplate)

    override fun replaceHeaders(source: Headers): Request = RoutedRequest(delegate.replaceHeaders(source), xUriTemplate)

    override fun removeHeader(name: String): Request = RoutedRequest(delegate.removeHeader(name), xUriTemplate)

    override fun removeHeaders(prefix: String): Request = RoutedRequest(delegate.removeHeaders(prefix), xUriTemplate)

    override fun body(body: Body): Request = RoutedRequest(delegate.body(body), xUriTemplate)

    override fun body(body: String): Request = RoutedRequest(delegate.body(body), xUriTemplate)

    override fun body(body: InputStream, length: Long?): Request = RoutedRequest(delegate.body(body, length), xUriTemplate)
}

class RoutedResponse(private val delegate: Response, override val xUriTemplate: UriTemplate) : Response by delegate, ResponseWithRoute {
    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun header(name: String, value: String?): Response = RoutedResponse(delegate.header(name, value), xUriTemplate)

    override fun headers(headers: Headers): Response = RoutedResponse(delegate.headers(headers), xUriTemplate)

    override fun replaceHeader(name: String, value: String?): Response = RoutedResponse(delegate.replaceHeader(name, value), xUriTemplate)

    override fun replaceHeaders(source: Headers): Response = RoutedResponse(delegate.replaceHeaders(source), xUriTemplate)

    override fun removeHeader(name: String): Response = RoutedResponse(delegate.removeHeader(name), xUriTemplate)

    override fun removeHeaders(prefix: String): Response = RoutedResponse(delegate.removeHeaders(prefix), xUriTemplate)

    override fun body(body: Body): Response = RoutedResponse(delegate.body(body), xUriTemplate)

    override fun body(body: String): Response = RoutedResponse(delegate.body(body), xUriTemplate)

    override fun body(body: InputStream, length: Long?): Response = RoutedResponse(delegate.body(body, length), xUriTemplate)

    override fun status(new: Status): Response = RoutedResponse(delegate.status(new), xUriTemplate)
}
