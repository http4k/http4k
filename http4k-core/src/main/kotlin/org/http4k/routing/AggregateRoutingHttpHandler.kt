package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then

internal data class AggregateRoutingHttpHandler(
    private val list: List<RoutingHttpHandler>,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotMatchedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    constructor(vararg list: RoutingHttpHandler) : this(list.toList())

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is RouterMatch.MatchingHandler -> matchResult(request)
        is RouterMatch.MethodNotMatched -> methodNotMatchedHandler(request)
        is RouterMatch.Unmatched -> notFoundHandler(request)
        is RouterMatch.Matched -> notFoundHandler(request)
    }

    override fun match(request: Request): RouterMatch = list.asSequence()
        .map { next -> next.match(request) }
        .sorted()
        .firstOrNull() ?: RouterMatch.Unmatched

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(list = list.map { it.withFilter(new) }, notFoundHandler = new.then(notFoundHandler), methodNotMatchedHandler = new.then(methodNotMatchedHandler))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(list = list.map { it.withBasePath(new) })
}
