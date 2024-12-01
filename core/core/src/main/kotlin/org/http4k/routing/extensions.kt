package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.routing.RouterDescription.Companion.unavailable
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

/**
 * Convert any predicate on a request into a router
 */
fun ((Request) -> Boolean).asRouter(description: String = unavailable.description): Router =
    PredicateRouter(this, description)

internal class PredicateRouter(private val predicate: (Request) -> Boolean, rawDescription: String) : Router {
    override val description = RouterDescription(rawDescription)
    override fun match(request: Request): RouterMatch =
        if (predicate(request)) MatchedWithoutHandler(description) else Unmatched(description)

    override fun toString() = description.friendlyToString()
}

fun Request.path(name: String): String? = when (this) {
    is RoutedMessage -> xUriTemplate.extract(uri.path)[name]
    else -> throw IllegalStateException("Request was not routed, so no uri-template present")
}

fun Method.asRouter(): Router = MethodRouter(this)

internal class MethodRouter(private val method: Method) : Router {
    override fun match(request: Request): RouterMatch = when (method) {
        request.method -> MatchedWithoutHandler(description)
        else -> MethodNotMatched(description)
    }

    override val description = RouterDescription("method == $method")

    override fun withBasePath(new: String): Router = this

    override fun toString() = description.friendlyToString()
}

fun Method.and(that: Router) = asRouter().and(that)

infix fun Method.bind(routingHandler: RoutingHttpHandler) = asRouter() bind routingHandler
infix fun Method.bind(httpHandler: HttpHandler) = asRouter() bind httpHandler
