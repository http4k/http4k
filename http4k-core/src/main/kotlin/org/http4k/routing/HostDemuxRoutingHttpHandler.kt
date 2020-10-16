package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.then

internal class HostDemuxRoutingHttpHandler(
    private val hosts: Map<String, RoutingHttpHandler>,
    private val notFoundHandler: HttpHandler = routeNotFoundHandler,
    private val methodNotAllowedHandler: HttpHandler = routeMethodNotAllowedHandler
) : RoutingHttpHandler {
    override fun invoke(p1: Request) = when (val result = match(p1)) {
        is RouterMatch.MatchingHandler -> result(p1)
        is RouterMatch.MethodNotMatched -> methodNotAllowedHandler(p1)
        is RouterMatch.Unmatched -> notFoundHandler(p1)
    }

    override fun match(request: Request) = request.header("host")
        ?.let { hosts[it]?.match(request) }
        ?: RouterMatch.Unmatched

    override fun withBasePath(new: String) = HostDemuxRoutingHttpHandler(
        hosts.mapValues { it.value.withBasePath(new) },
        notFoundHandler, methodNotAllowedHandler
    )

    override fun withFilter(new: Filter) = HostDemuxRoutingHttpHandler(
        hosts.mapValues { it.value.withFilter(new) },
        new.then(notFoundHandler),
        new.then(methodNotAllowedHandler)
    )
}
