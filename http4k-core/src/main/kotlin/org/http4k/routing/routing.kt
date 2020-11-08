package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.websocket.WsConsumer

/**
 * Composite HttpHandler which can potentially service many different URL patterns. Should
 * return a 404 Response if it cannot service a particular Request.
 *
 * Note that generally there should be no reason for the API user to implement this interface over and above the
 * implementations that already exist. The interface is public only because we have not found a way to hide it from
 * the API user in an API-consistent manner.
 */
interface RoutingHttpHandler : Router, HttpHandler {
    override fun withFilter(new: Filter): RoutingHttpHandler
    override fun withBasePath(new: String): RoutingHttpHandler
}

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler = routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(OrRouter(list.toList()))

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
                        else -> MethodNotMatched
                    }
                }
            }
            else -> RouterRoutingHttpHandler(TemplatingRouter(method, UriTemplate.from(path), action))
        }
}

infix fun String.bind(method: Method): PathMethod = PathMethod(this, method)

infix fun String.bind(httpHandler: RoutingHttpHandler): RoutingHttpHandler = httpHandler.withBasePath(this)

infix fun String.bind(action: HttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(TemplatingRouter(null, UriTemplate.from(this), action))

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler = TemplateRoutingWsHandler(UriTemplate.from(this), consumer)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

/**
 * For routes where certain queries are required for correct operation. Router is composable.
 */
fun queries(vararg names: String): Router = Router { request -> names.all { request.query(it) != null }.asRouterMatch() }

/**
 * For routes where certain headers are required for correct operation. Router is composable.
 */
fun headers(vararg names: String): Router = Router { request -> names.all { request.header(it) != null }.asRouterMatch() }

/**
 * Matches the Host header to a matching Handler.
 */
fun hostDemux(head: Pair<String, RoutingHttpHandler>, vararg tail: Pair<String, RoutingHttpHandler>): RoutingHttpHandler {
    val hostHandlerPairs = listOf(head) + tail
    return routes(*hostHandlerPairs.map { Router { req: Request -> (req.header("host") == it.first).asRouterMatch() } bind it.second }.toTypedArray())
}

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(and(Passthrough(handler)))
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(and(handler))
infix fun Router.and(that: Router): Router = AndRouter(this, that)

internal class Passthrough(private val handler: HttpHandler) : RoutingHttpHandler {
    override fun withFilter(new: Filter) = when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> Passthrough(new.then(handler))
    }

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> this
    }

    override fun match(request: Request): RouterMatch = RouterMatch.MatchingHandler(handler)

    override fun invoke(request: Request): Response = handler(request)
}

private fun Boolean.asRouterMatch() = if (this) RouterMatch.MatchedWithoutHandler else RouterMatch.Unmatched
