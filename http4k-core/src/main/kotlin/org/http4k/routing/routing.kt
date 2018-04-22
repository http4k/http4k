package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import java.io.InputStream

/**
 * Provides matching of a Request to an HttpHandler which can service it.
 */
interface Router {
    fun match(request: Request): HttpHandler?
}

/**
 * Composite HttpHandler which can service many different URL paths.
 */
interface RoutingHttpHandler : Router, HttpHandler {
    /**
     * Apply the Filter to all received requests before servicing them.
     */
    fun withFilter(new: Filter): RoutingHttpHandler

    /**
     * Prepend the existing matching paths with the one passed.
     */
    fun withBasePath(new: String): RoutingHttpHandler
}

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler = routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = AggregateRoutingHttpHandler(*list)

fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>): StaticRoutingHttpHandler =
    StaticRoutingHttpHandler("", resourceLoader, extraPairs.asList().toMap())

interface RoutingWsHandler : WsHandler {
    fun withBasePath(new: String): RoutingWsHandler
}

fun websockets(vararg list: RoutingWsHandler): RoutingWsHandler = object : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = list.firstOrNull { it.invoke(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingWsHandler = websockets(*list.map { it.withBasePath(new) }.toTypedArray())
}

fun Request.path(name: String): String? = when (this) {
    is RoutedRequest -> xUriTemplate.extract(uri.path)[name]
    else -> throw IllegalStateException("Request was not routed, so no uri-template not present")
}

data class PathMethod(val path: String, val method: Method) {
    infix fun to(action: HttpHandler): RoutingHttpHandler = TemplateRoutingHttpHandler(method, UriTemplate.from(path), action)
    infix fun to(action: StaticRoutingHttpHandler): RoutingHttpHandler = action.withBasePath(path).let {
        object : RoutingHttpHandler by it {
            override fun match(request: Request): HttpHandler? = when (method) {
                request.method -> it.match(request)
                else -> null
            }
        }
    }
}

infix fun String.bind(method: Method): PathMethod = PathMethod(this, method)

infix fun String.bind(httpHandler: RoutingHttpHandler): RoutingHttpHandler = httpHandler.withBasePath(this)

infix fun String.bind(action: HttpHandler): RoutingHttpHandler = TemplateRoutingHttpHandler(null, UriTemplate.from(this), action)

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler = TemplateRoutingWsHandler(UriTemplate.from(this), consumer)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

data class RoutedRequest(private val delegate: Request, val xUriTemplate: UriTemplate) : Request by delegate {
    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun method(method: Method): Request = RoutedRequest(delegate.method(method), xUriTemplate)

    override fun uri(uri: Uri): Request = RoutedRequest(delegate.uri(uri), xUriTemplate)

    override fun query(name: String, value: String): Request = RoutedRequest(delegate.query(name, value), xUriTemplate)

    override fun header(name: String, value: String?): Request = RoutedRequest(delegate.header(name, value), xUriTemplate)

    override fun headers(headers: Headers): Request = RoutedRequest(delegate.headers(headers), xUriTemplate)

    override fun replaceHeader(name: String, value: String?): Request = RoutedRequest(delegate.replaceHeader(name, value), xUriTemplate)

    override fun removeHeader(name: String): Request = RoutedRequest(delegate.removeHeader(name), xUriTemplate)

    override fun body(body: Body): Request = RoutedRequest(delegate.body(body), xUriTemplate)

    override fun body(body: String): Request = RoutedRequest(delegate.body(body), xUriTemplate)

    override fun body(body: InputStream, length: Long?): Request = RoutedRequest(delegate.body(body, length), xUriTemplate)
}

class RoutedResponse(private val delegate: Response, val xUriTemplate: UriTemplate) : Response by delegate {
    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun header(name: String, value: String?): Response = RoutedResponse(delegate.header(name, value), xUriTemplate)

    override fun replaceHeader(name: String, value: String?): Response = RoutedResponse(delegate.replaceHeader(name, value), xUriTemplate)

    override fun removeHeader(name: String): Response = RoutedResponse(delegate.removeHeader(name), xUriTemplate)

    override fun body(body: Body): Response = RoutedResponse(delegate.body(body), xUriTemplate)

    override fun body(body: String): Response = RoutedResponse(delegate.body(body), xUriTemplate)

    override fun body(body: InputStream, length: Long?): Response = RoutedResponse(delegate.body(body, length), xUriTemplate)
}