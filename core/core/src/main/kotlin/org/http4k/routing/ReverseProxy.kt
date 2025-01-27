package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request

/**
 * Simple Reverse Proxy which will split and direct traffic to the appropriate
 * HttpHandler based on the content of the Host header
 */
fun reverseProxy(vararg hostToHandler: Pair<String, HttpHandler>): HttpHandler =
    reverseProxyRouting(*hostToHandler)

/**
 * Simple Reverse Proxy. Exposes routing.
 */
fun reverseProxyRouting(vararg hostToHandler: Pair<String, HttpHandler>): RoutingHttpHandler =
    RoutingHttpHandler(
        hostToHandler.flatMap { (host, handler) ->
            when (handler) {
                is RoutingHttpHandler -> handler.routes.map { it.withRouter(hostHeaderOrUriHost(host)) }
                else -> listOf(SimpleRouteMatcher(hostHeaderOrUriHost(host), handler))
            }
        }
    )

private fun hostHeaderOrUriHost(host: String) =
    { req: Request ->
        (req.headerValues("host").firstOrNull() ?: req.uri.authority).contains(host)
    }.asRouter("host header or uri host = $host")
