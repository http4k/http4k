package org.http4k.routing

import org.http4k.core.Request
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.Unmatched

/**
 * Convert any predicate on a request into a router
 */
fun ((Request) -> Boolean).asRouter(): Router = Router { r: Request ->
    if (this(r)) MatchedWithoutHandler else Unmatched
}

fun Request.path(name: String): String? = when (this) {
    is RoutedRequest -> xUriTemplate.extract(uri.path)[name]
    else -> throw IllegalStateException("Request was not routed, so no uri-template present")
}
