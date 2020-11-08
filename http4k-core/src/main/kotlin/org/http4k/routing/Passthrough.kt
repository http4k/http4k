package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then

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
