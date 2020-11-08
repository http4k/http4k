package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate

/**
 * Composite HttpHandler which can potentially service many different URL patterns. Should
 * return a 404 Response if it cannot service a particular Request.
 *
 * Note that generally there should be no reason for the API user to implement this interface over and above the
 * implementations that already exist. The interface is public only because we have not found a way to hide it from
 * the API user in an API-consistent manner.
 */
interface RoutingHttpHandler : Router, HttpHandler {
    override fun withFilter(new: Filter): RoutingHttpHandler
    override fun withBasePath(new: String): RoutingHttpHandler
}

data class PathMethod(val path: String, val method: Method?) {
    infix fun to(action: HttpHandler): RoutingHttpHandler =
        when (action) {
            is StaticRoutingHttpHandler -> action.withBasePath(path).let {
                object : RoutingHttpHandler by it {
                    override fun match(request: Request) = when (method) {
                        null, request.method -> it.match(request)
                        else -> RouterMatch.MethodNotMatched
                    }
                }
            }
            else -> RouterRoutingHttpHandler(TemplatingRouter(method, UriTemplate.from(path), action))
        }
}
