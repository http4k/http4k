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
fun queries(vararg names: String): Router = Router { req: Request -> names.all { req.query(it) != null }.asRouterMatch() }

/**
 * For routes where certain headers are required for correct operation. RequestMatch is composable.
 */
fun headers(vararg names: String): Router = Router { req: Request -> names.all { req.header(it) != null }.asRouterMatch() }


/**
 * Matches the Host header to a matching Handler.
 */
fun hostDemux(head: Pair<String, RoutingHttpHandler>, vararg tail: Pair<String, RoutingHttpHandler>): RoutingHttpHandler {
    val hostHandlerPairs = listOf(head) + tail
    return routes(*hostHandlerPairs.map { Router { req: Request -> (req.header("host") == it.first).asRouterMatch() } bind it.second }.toTypedArray())
}

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = PredicatedHandler(this, handler)
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RequestMatchRoutingHttpHandler(this, handler)
infix fun Router.and(that: Router): Router = Router { listOf(this, that).fold(MatchedWithoutHandler as RouterMatch) { acc, next -> acc.and(next.match(it)) } }

internal class PredicatedHandler(private val predicate: Router, private val handler: HttpHandler) : RoutingHttpHandler {
    override fun withFilter(new: Filter) = PredicatedHandler(predicate, when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> new.then(handler)
    })

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> throw UnsupportedOperationException("Cannot apply new base path without binding to an HTTP verb")
    }

    override fun match(request: Request) = if (predicate.match(request) == MatchedWithoutHandler) MatchingHandler(handler) else Unmatched

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

    override fun match(request: Request) = if (matched.match(request) != MatchedWithoutHandler) Unmatched else httpHandler.match(request)

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult(request)
        is MatchedWithoutHandler -> httpHandler(request)
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

private fun Boolean.asRouterMatch() = if (this) MatchedWithoutHandler else Unmatched
