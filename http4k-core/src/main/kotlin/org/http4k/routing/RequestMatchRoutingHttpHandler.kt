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

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = RouterRoutingHandler(and(Passthrough(handler)))
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RouterRoutingHandler(and(handler))
infix fun Router.and(that: Router): Router = Router { listOf(this, that).fold(MatchedWithoutHandler as RouterMatch) { acc, next -> acc.and(next.match(it)) } }

class Passthrough(private val handler: HttpHandler) : RoutingHttpHandler {
    override fun withFilter(new: Filter) = when (handler) {
        is RoutingHttpHandler -> handler.withFilter(new)
        else -> Passthrough(new.then(handler))
    }

    override fun withBasePath(new: String) = when (handler) {
        is RoutingHttpHandler -> handler.withBasePath(new)
        else -> throw UnsupportedOperationException("Cannot apply new base path")
    }

    override fun match(request: Request): RouterMatch = MatchingHandler(handler)

    override fun invoke(request: Request): Response = handler(request)
}

private fun Boolean.asRouterMatch() = if (this) MatchedWithoutHandler else Unmatched
