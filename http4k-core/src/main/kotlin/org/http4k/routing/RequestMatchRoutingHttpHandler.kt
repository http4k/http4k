package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.Unmatched

/**
 * For routes where certain queries are required for correct operation. RequestMatch is composable.
 */
fun queries(vararg names: String): Router = object : Router {
    override fun match(request: Request) = names.all { request.query(it) != null }.asRouterMatch()

    override fun withBasePath(new: String) = Prefix(new).and(this)

    override fun withFilter(new: Filter) = this
}

/**
 * For routes where certain headers are required for correct operation. RequestMatch is composable.
 */
fun headers(vararg names: String): Router = object : Router {
    override fun match(request: Request) = names.all { request.header(it) != null }.asRouterMatch()

    override fun withBasePath(new: String) = Prefix(new).and(this)

    override fun withFilter(new: Filter) = this
}

/**
 * Matches the Host header to a matching Handler.
 */
fun hostDemux(head: Pair<String, RoutingHttpHandler>, vararg tail: Pair<String, RoutingHttpHandler>): RoutingHttpHandler {
    val hostHandlerPairs = listOf(head) + tail
    return routes(*hostHandlerPairs.map { Router { req: Request -> (req.header("host") == it.first).asRouterMatch() } bind it.second }.toTypedArray())
}

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = RequestMatchRoutingHttpHandler(this, Passthrough(handler))
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RequestMatchRoutingHttpHandler(this, handler)
infix fun Router.and(that: Router): Router = AndRouter(this, that)

internal data class RequestMatchRoutingHttpHandler(
    private val router: Router,
    private val httpHandler: RoutingHttpHandler,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request) = if (router.match(request) != MatchedWithoutHandler) Unmatched else httpHandler.match(request)

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult(request)
        is MatchedWithoutHandler -> httpHandler(request)
        is RouterMatch.MethodNotMatched -> methodNotAllowedHandler(request)
        is Unmatched -> notFoundHandler(request)
    }

    override fun withFilter(new: Filter): RoutingHttpHandler = RequestMatchRoutingHttpHandler(
        router.withFilter(new),
        httpHandler.withFilter(new),
        new.then(notFoundHandler),
        new.then(methodNotAllowedHandler)
    )

    override fun withBasePath(new: String): RoutingHttpHandler =
        copy(
            router.withBasePath(new),
            httpHandler = httpHandler.withBasePath(new))
}

class Passthrough(private val handler: HttpHandler) : RoutingHttpHandler {
    override fun withFilter(new: Filter) = when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> Passthrough(new.then(handler))
    }

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> this
    }

    override fun match(request: Request): RouterMatch = MatchingHandler(handler)

    override fun invoke(request: Request): Response = handler(request)
}


private fun Boolean.asRouterMatch() = if (this) MatchedWithoutHandler else Unmatched
