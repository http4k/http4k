package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched

/**
 * Composite HttpHandler which can potentially service many different URL patterns. Should
 * return a 404 Response if it cannot service a particular Request.
 *
 * Note that generally there should be no reason for the API user to implement this interface over and above the
 * implementations that already exist. The interface is public only because we have not found a way to hide it from
 * the API user in an API-consistent manner.
 */
data class RoutingHttpHandler(
    val routes: List<RouteMatcher>
) : HttpHandler {
    override fun invoke(request: Request) = routes
        .map { it.match(request) }
        .sortedBy(HttpMatchResult::priority)
        .first().handler(request)

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(new: Filter) = copy(routes = routes.map { it.withFilter(new) })

    fun withPredicate(predicate: Predicate) =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString() = routes.sortedBy(RouteMatcher::toString).joinToString("\n")
}

interface RouteMatcher {
    fun match(request: Request): HttpMatchResult
    fun withBasePath(prefix: String): RouteMatcher
    fun withPredicate(other: Predicate): RouteMatcher
    fun withFilter(new: Filter): RouteMatcher
}

data class TemplatedHttpRoute(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    private val predicate: Predicate = All,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher {
    init {
        require(handler !is RoutingHttpHandler)
    }

    override fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = predicate(request)) {
            is Matched -> HttpMatchResult(0, AddUriTemplate(uriTemplate).then(filter).then(handler))
            is NotMatched -> HttpMatchResult(1, filter.then { _: Request -> Response(result.status) })
        }

        else -> HttpMatchResult(2, filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    override fun withPredicate(other: Predicate) = copy(predicate = predicate.and(other))

    override fun withFilter(new: Filter): RouteMatcher = copy(filter = new.then(filter))

    override fun toString() = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
        {
            RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}

data class PredicateRouteMatcher(
    private val handler: HttpHandler,
    private val predicate: Predicate,
    private val filter: Filter = Filter.NoOp
) : RouteMatcher {

    override fun match(request: Request) = when (val result = predicate(request)) {
        is Matched -> HttpMatchResult(0, filter.then(handler))
        is NotMatched -> HttpMatchResult(1, filter.then { _: Request -> Response(result.status) })
    }

    override fun withBasePath(prefix: String): RouteMatcher = TemplatedHttpRoute(UriTemplate.from(prefix), handler)

    override fun withPredicate(other: Predicate): RouteMatcher = PredicateRouteMatcher(handler, predicate.and(other))
    override fun withFilter(new: Filter): RouteMatcher = copy(filter = new.then(filter))
}

data class HttpMatchResult(val priority: Int, val handler: HttpHandler)

data class HttpPathMethod(val path: String, val method: Method) {
    infix fun to(handler: HttpHandler) = when (handler) {
        is RoutingHttpHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutingHttpHandler(listOf(TemplatedHttpRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}
