package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched

internal data class RouterBasedHttpHandler(
    private val router: Router,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler
) : RoutingHttpHandler, Router by router {

    override fun invoke(request: Request): Response = when (val matchResult = match(request)) {
        is MatchingHandler -> matchResult
        is MethodNotMatched -> methodNotAllowedHandler
        else -> notFoundHandler
    }(request)

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(
        router.withFilter(new),
        notFoundHandler = new.then(notFoundHandler),
        methodNotAllowedHandler = new.then(methodNotAllowedHandler)
    )

    override fun withBasePath(new: String): RoutingHttpHandler = copy(router = router.withBasePath(new))
}

internal val routeNotFoundHandler: HttpHandler = { Response(NOT_FOUND.description("Route not found")) }

internal val routeMethodNotAllowedHandler: HttpHandler = { Response(METHOD_NOT_ALLOWED.description("Method not allowed")) }
