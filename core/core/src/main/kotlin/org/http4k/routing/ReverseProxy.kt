package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.routing.ReverseProxyHostMatcher.Companion.Exact

/**
 * Simple Reverse Proxy which will split and direct traffic to the appropriate
 * HttpHandler based on the content of the Host header.
 *
 * Optionally takes a [ReverseProxyHostMatcher] to determine how the host header is matched: defaults to an exact match.
 */
fun reverseProxy(
    vararg hostToHandler: Pair<String, HttpHandler>, matcher: ReverseProxyHostMatcher = Exact
): HttpHandler =
    reverseProxyRouting(*hostToHandler, matcher = matcher)

/**
 * Simple Reverse Proxy. Exposes routing.
 *
 * Optionally takes a [ReverseProxyHostMatcher] to determine how the host header is matched: defaults to an exact match.
 */
fun reverseProxyRouting(
    vararg hostToHandler: Pair<String, HttpHandler>,
    matcher: ReverseProxyHostMatcher = Exact
): RoutingHttpHandler =
    RoutingHttpHandler(
        hostToHandler.flatMap { (host, handler) ->
            when (handler) {
                is RoutingHttpHandler -> handler.routes.map { it.withRouter(matcher.matchesHostHeaderOrUriAgainst(host)) }
                else -> listOf(SimpleRouteMatcher(matcher.matchesHostHeaderOrUriAgainst(host), handler))
            }
        }
    )

/**
 * A [ReverseProxyHostMatcher] is used to determine how the host header is matched against the extracted host from the request.
 */
fun interface ReverseProxyHostMatcher {
    operator fun invoke(host: String, extracted: String): Boolean

    companion object {
        /**
         * Matches when the request's host header/authority contains the configured host as a substring.
         */
        val Contains = ReverseProxyHostMatcher { host, extracted -> host.contains(extracted) }

        /**
         * Matches when the request's host header/authority is equal to the configured host, ignoring case
         * (per RFC 7230 §2.7.3). This is the default.
         */
        val Exact = ReverseProxyHostMatcher { host, extracted -> extracted.equals(host, ignoreCase = true) }
    }
}

private fun ReverseProxyHostMatcher.matchesHostHeaderOrUriAgainst(host: String) =
    { req: Request -> this(req.headerValues("host").firstOrNull() ?: req.uri.authority, host) }
        .asRouter("host header or uri host = $host")
