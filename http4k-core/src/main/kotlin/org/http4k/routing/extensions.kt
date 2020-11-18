package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.Unmatched

/**
 * Convert any predicate on a request into a router
 */
fun ((Request) -> Boolean).asRouter(): Router = object : Router {
    override fun match(request: Request): RouterMatch =
        if (this@asRouter(request)) MatchedWithoutHandler(description) else Unmatched(description)
}

fun Request.path(name: String): String? = when (this) {
    is RoutedRequest -> xUriTemplate.extract(uri.path)[name]
    else -> throw IllegalStateException("Request was not routed, so no uri-template present")
}

fun Method.asRouter() = object : Router {
    override fun match(request: Request): RouterMatch =
        if (this@asRouter == request.method) MatchedWithoutHandler(description) else RouterMatch.MethodNotMatched(description)

    override val description = RouterDescription("method == ${this@asRouter}")
}

fun Method.and(that: Router) = asRouter().and(that)
