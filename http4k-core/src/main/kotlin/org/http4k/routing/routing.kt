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
fun interface Router {
    /**
     * Attempt to supply an HttpHandler which can service the passed request.
     */
    fun match(request: Request): RouterMatch
}

sealed class RouterMatch(private val priority: Int) : Comparable<RouterMatch> {
    data class MatchingHandler(private val httpHandler: HttpHandler) : RouterMatch(0), HttpHandler {
        override fun invoke(request: Request): Response = httpHandler(request)
    }

    object MethodNotMatched : RouterMatch(1)
    object Unmatched : RouterMatch(2)

    override fun compareTo(other: RouterMatch): Int = priority.compareTo(other.priority)
}

/**
 * Composite HttpHandler which can potentially service many different URL patterns. Should
 * return a 404 Response if it cannot service a particular Request.
 *
 * Note that generally there should be no reason for the API user to implement this interface over and above the
 * implementations that already exist. The interface is public only because we have not found a way to hide it from
 * the API user in an API-consistent manner.
 */
interface RoutingHttpHandler : Router, HttpHandler {
    /**
     * Returns a RoutingHttpHandler which applies the passed Filter to all received requests before servicing them.
     * To follow the trend of immutability, this will generally be a new instance.
     */
    fun withFilter(new: Filter): RoutingHttpHandler

    /**
     * Returns a RoutingHttpHandler which prepends the passed base path to the logic determining the match()
     * To follow the trend of immutability, this will generally be a new instance.
     */
    fun withBasePath(new: String): RoutingHttpHandler
}

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler = routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = AggregateRoutingHttpHandler(*list)

/**
 * Serve static content using the passed ResourceLoader. Note that for security, by default ONLY mime-types registered in
 * mime.types (resource file) will be served. All other types are registered as application/octet-stream and are not served.
 */
fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraFileExtensionToContentTypes: Pair<String, ContentType>): RoutingHttpHandler = StaticRoutingHttpHandler("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())

/**
 * For SPAs we serve static content as usual, or fall back to the index page. The resource loader is configured to look at
 * /public package (on the Classpath).
 */
fun singlePageApp(resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"), vararg extraFileExtensionToContentTypes: Pair<String, ContentType>): RoutingHttpHandler =
    SinglePageAppRoutingHandler("", StaticRoutingHttpHandler("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap()))

/**
 * For routes where certain queries are required for correct operation. RequestMatch is composable.
 */
fun queries(vararg names: String): RequestMatch = { req -> names.all { req.query(it) != null } }

/**
 * For routes where certain headers are required for correct operation. RequestMatch is composable.
 */
fun headers(vararg names: String): RequestMatch = { req -> names.all { req.header(it) != null } }

typealias RequestMatch = (Request) -> Boolean

infix fun RequestMatch.bind(handler: HttpHandler): RoutingHttpHandler = PredicatedHandler(this, handler)
infix fun RequestMatch.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RequestMatchRoutingHttpHandler(this, handler)
infix fun RequestMatch.and(that: RequestMatch): RequestMatch = { listOf(this, that).fold(true) { acc, next -> acc && next(it) } }

/**
 * Matches the Host header to a matching Handler.
 */
fun hostDemux(head: Pair<String, RoutingHttpHandler>, vararg tail: Pair<String, RoutingHttpHandler>): RoutingHttpHandler {
    val hostHandlerPairs = listOf(head) + tail
    return routes(*hostHandlerPairs.map { { req: Request -> req.header("host") == it.first } bind it.second }.toTypedArray())
}

interface RoutingWsHandler : WsHandler {
    fun withBasePath(new: String): RoutingWsHandler
}

fun websockets(ws: WsConsumer): WsHandler = { ws }

fun websockets(vararg list: RoutingWsHandler): RoutingWsHandler = object : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = list.firstOrNull { it(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingWsHandler = websockets(*list.map { it.withBasePath(new) }.toTypedArray())
}

fun Request.path(name: String): String? = when (this) {
    is RoutedRequest -> xUriTemplate.extract(uri.path)[name]
    else -> throw IllegalStateException("Request was not routed, so no uri-template present")
}

data class PathMethod(val path: String, val method: Method?) {
    infix fun to(action: HttpHandler): RoutingHttpHandler =
        when (action) {
            is StaticRoutingHttpHandler -> action.withBasePath(path).let {
                object : RoutingHttpHandler by it {
                    override fun match(request: Request) = when (method) {
                        null, request.method -> it.match(request)
                        else -> RouterMatch.MethodNotMatched
                    }
                }
            }
            else -> TemplateRoutingHttpHandler(method, UriTemplate.from(path), action)
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

    override fun query(name: String, value: String?): Request = RoutedRequest(delegate.query(name, value), xUriTemplate)

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
