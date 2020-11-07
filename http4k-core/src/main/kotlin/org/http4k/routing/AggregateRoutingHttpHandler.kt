package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then

internal data class AggregateRoutingHttpHandler(
    private val router: Router,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotMatchedHandler: HttpHandler = routeMethodNotAllowedHandler) : RoutingHttpHandler {

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is RouterMatch.MatchingHandler -> matchResult(request)
        is RouterMatch.MethodNotMatched -> methodNotMatchedHandler(request)
        is RouterMatch.Unmatched -> notFoundHandler(request)
        is RouterMatch.MatchedWithoutHandler -> notFoundHandler(request)
    }

    override fun match(request: Request): RouterMatch = router.match(request)

    override fun withFilter(new: Filter): RoutingHttpHandler =
        copy(router = router.withFilter(new),
            notFoundHandler = new.then(notFoundHandler),
            methodNotMatchedHandler = new.then(methodNotMatchedHandler))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(router = router.withBasePath(new))
}

class OrRouter(private val list: List<Router>) : Router {
    override fun match(request: Request) = list.asSequence()
        .map { next -> next.match(request) }
        .sorted()
        .firstOrNull() ?: RouterMatch.Unmatched

    override fun withFilter(new: Filter) =
        OrRouter(list.map { it.withFilter(new) })

    override fun withBasePath(new: String) = OrRouter(list.map { it.withBasePath(new) })
}
