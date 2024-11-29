package org.http4k.routing.experimental

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.routeMethodNotAllowedHandler
import org.http4k.routing.routeNotFoundHandler

data class RoutedHttpHandler(
    val routes: List<TemplatedRoute>,
    private val routeNotFound: HttpHandler = routeNotFoundHandler,
    private val routeMethodNotAllowed: HttpHandler = routeMethodNotAllowedHandler
) : HttpHandler {
    override fun invoke(request: Request) = routes
        .map { it.match(request) }
        .sortedBy(RoutingMatchResult::priority)
        .first()
        .toHandler()(request)

    fun withBasePath(prefix: String): RoutedHttpHandler = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(filter: Filter): RoutedHttpHandler = copy(
        routes = routes.map { it.withFilter(filter) },
        routeNotFound = filter.then(routeNotFound),
        routeMethodNotAllowed = filter.then(routeMethodNotAllowed)
    )

    fun withPredicate(predicate: Predicate): RoutedHttpHandler =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString(): String = routes.sortedBy(TemplatedRoute::toString).joinToString("\n")

    private fun RoutingMatchResult.toHandler() =
        when (this) {
            is RoutingMatchResult.Matched -> handler
            is RoutingMatchResult.MethodNotMatched -> routeMethodNotAllowed
            is RoutingMatchResult.NotFound -> routeNotFound
        }
}

data class TemplatedRoute(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    private val predicate: Predicate = Any
) {
    init {
        require(handler !is RoutedHttpHandler)
    }

    internal fun match(request: Request): RoutingMatchResult =
        if (uriTemplate.matches(request.uri.path)) {
            if (!predicate(request))
                RoutingMatchResult.MethodNotMatched
            else
                RoutingMatchResult.Matched(AddUriTemplate(uriTemplate).then(handler))
        } else
            RoutingMatchResult.NotFound

    fun withBasePath(prefix: String): TemplatedRoute = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withFilter(filter: Filter): TemplatedRoute = copy(handler = filter.then(handler))

    fun withPredicate(other: Predicate): TemplatedRoute = copy(predicate = predicate.and(other))

    override fun toString(): String = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
        {
            RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}

internal sealed class RoutingMatchResult(val priority: Int) {
    data class Matched(val handler: HttpHandler) : RoutingMatchResult(0)
    data object MethodNotMatched : RoutingMatchResult(1)
    data object NotFound : RoutingMatchResult(2)
}

data class NewPathMethod(val path: String, val method: Method) {
    infix fun to(handler: HttpHandler) =
        when (handler) {
            is RoutedHttpHandler ->
                handler.withPredicate(method.asPredicate()).withBasePath(path)

            else -> RoutedHttpHandler(listOf(TemplatedRoute(UriTemplate.from(path), handler, method.asPredicate())))
        }
}
