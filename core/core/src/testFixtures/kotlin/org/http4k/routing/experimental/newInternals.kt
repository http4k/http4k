package org.http4k.routing.experimental

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.experimental.PredicateResult.*

data class RoutedHttpHandler(
    val routes: List<TemplatedRoute>,
    private val filter: Filter = Filter.NoOp
) : HttpHandler {
    override fun invoke(request: Request) = filter.then(routes
        .map { it.match(request) }
        .sortedBy(RoutingMatchResult::priority)
        .first().handler)(request)

    fun withBasePath(prefix: String): RoutedHttpHandler = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(filter: Filter): RoutedHttpHandler = copy(filter = filter.then(this.filter))

    fun withPredicate(predicate: Predicate): RoutedHttpHandler =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString(): String = routes.sortedBy(TemplatedRoute::toString).joinToString("\n")
}

data class TemplatedRoute(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    private val predicate: Predicate = Any
) {
    init {
        require(handler !is RoutedHttpHandler)
    }

    internal fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = predicate(request)) {
            is Matched -> RoutingMatchResult(0, AddUriTemplate(uriTemplate).then(handler))
            is NotMatched -> RoutingMatchResult(1) { _: Request -> Response(result.status) }
        }

        else -> RoutingMatchResult(2) { _: Request -> Response(NOT_FOUND) }
    }

    fun withBasePath(prefix: String): TemplatedRoute = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withPredicate(other: Predicate): TemplatedRoute = copy(predicate = predicate.and(other))

    override fun toString(): String = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
        {
            RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}

internal data class RoutingMatchResult(val priority: Int, val handler: HttpHandler)

data class NewPathMethod(val path: String, val method: Method) {
    infix fun to(handler: HttpHandler) = when (handler) {
        is RoutedHttpHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutedHttpHandler(listOf(TemplatedRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}
