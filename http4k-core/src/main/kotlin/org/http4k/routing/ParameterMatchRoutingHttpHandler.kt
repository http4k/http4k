package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then

sealed class ParameterMatch(private val predicate: (Request) -> Boolean) : (Request) -> Boolean by predicate {
    infix fun bind(handler: HttpHandler): RoutingHttpHandler = PredicatedHandler(predicate, handler)

    infix fun bind(handler: RoutingHttpHandler): RoutingHttpHandler = ParameterMatchRoutingHttpHandler(this, handler)
    infix fun and(that: ParameterMatch): ParameterMatch = Composite(this, that)

    internal class Query(vararg names: String) : ParameterMatch({ req -> names.all { req.query(it) != null } })
    internal class Header(vararg names: String) : ParameterMatch({ req -> names.all { req.header(it) != null } })
    internal class Composite(vararg parts: ParameterMatch) : ParameterMatch({ parts.fold(true) { acc, next -> acc && next(it) } })
}

internal class PredicatedHandler(private val predicate: (Request) -> Boolean, private val handler: HttpHandler) : RoutingHttpHandler {
    override fun withFilter(new: Filter) = throw UnsupportedOperationException("Not available at the top level")
    override fun withBasePath(new: String) = throw UnsupportedOperationException("Not available at the top level")
    override fun match(request: Request) = if (predicate(request)) RouterMatch.MatchingHandler(handler) else RouterMatch.Unmatched

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is RouterMatch.MatchingHandler -> matchResult(request)
        else -> routeNotFoundHandler(request)
    }
}

internal data class ParameterMatchRoutingHttpHandler(
    private val matched: (Request) -> Boolean,
    private val httpHandler: RoutingHttpHandler,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun match(request: Request) = if (!matched(request)) RouterMatch.Unmatched else httpHandler.match(request)

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is RouterMatch.MatchingHandler -> matchResult(request)
        is RouterMatch.MethodNotMatched -> methodNotAllowedHandler(request)
        is RouterMatch.Unmatched -> notFoundHandler(request)
    }

    override fun withFilter(new: Filter): RoutingHttpHandler = ParameterMatchRoutingHttpHandler(
        matched,
        new.then(httpHandler),
        new.then(notFoundHandler),
        new.then(methodNotAllowedHandler)
    )

    override fun withBasePath(new: String): RoutingHttpHandler =
        copy(httpHandler = httpHandler.withBasePath(new))
}
