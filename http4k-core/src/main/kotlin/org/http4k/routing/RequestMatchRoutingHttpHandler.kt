package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.Unmatched

private typealias RequestMatcher = (Request) -> RouterMatch

private fun Boolean.asRouterMatch() = if (this) RouterMatch.Matched else Unmatched

private fun RequestMatcher.asRouter() = Router { request -> this@asRouter(request) }

/**
 * For routes where certain queries are required for correct operation. RequestMatch is composable.
 */
fun queries(vararg names: String): Router = { req: Request -> names.all { req.query(it) != null }.asRouterMatch() }.asRouter()

/**
 * For routes where certain headers are required for correct operation. RequestMatch is composable.
 */
fun headers(vararg names: String): Router = { req: Request -> names.all { req.header(it) != null }.asRouterMatch() }.asRouter()


/**
 * Matches the Host header to a matching Handler.
 */
fun hostDemux(head: Pair<String, RoutingHttpHandler>, vararg tail: Pair<String, RoutingHttpHandler>): RoutingHttpHandler {
    val hostHandlerPairs = listOf(head) + tail
    return routes(*hostHandlerPairs.map { { req: Request -> (req.header("host") == it.first).asRouterMatch() }.asRouter() bind it.second }.toTypedArray())
}

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = PredicatedHandler(this, handler)
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RequestMatchRoutingHttpHandler(this, handler)
infix fun Router.and(that: Router): Router = { it: Request ->
    val initial: RouterMatch = RouterMatch.Matched
    listOf(this, that).fold(initial) { acc, next -> acc.and(next.match(it)) }
}.asRouter()

internal class PredicatedHandler(private val predicate: Router, private val handler: HttpHandler) : RoutingHttpHandler {
    override fun withFilter(new: Filter) = PredicatedHandler(predicate, when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> new.then(handler)
    })

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> throw UnsupportedOperationException("Cannot apply new base path without binding to an HTTP verb")
    }

    override fun match(request: Request) = if (predicate.match(request) == RouterMatch.Matched) MatchingHandler(handler) else Unmatched

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult(request)
        else -> routeNotFoundHandler(request)
    }
}

internal data class RequestMatchRoutingHttpHandler(
    private val matched: Router,
    private val httpHandler: RoutingHttpHandler,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request) = if (matched.match(request) != RouterMatch.Matched) Unmatched else httpHandler.match(request)

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult(request)
        is RouterMatch.Matched -> httpHandler(request)
        is RouterMatch.MethodNotMatched -> methodNotAllowedHandler(request)
        is Unmatched -> notFoundHandler(request)
    }

    override fun withFilter(new: Filter): RoutingHttpHandler = RequestMatchRoutingHttpHandler(
        matched,
        httpHandler.withFilter(new),
        new.then(notFoundHandler),
        new.then(methodNotAllowedHandler)
    )

    override fun withBasePath(new: String): RoutingHttpHandler =
        copy(httpHandler = httpHandler.withBasePath(new))
}
