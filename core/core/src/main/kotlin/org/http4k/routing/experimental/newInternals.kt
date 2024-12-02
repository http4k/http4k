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
import org.http4k.routing.experimental.PredicateResult.Matched
import org.http4k.routing.experimental.PredicateResult.NotMatched

data class RoutedHttpHandler(
    val routes: List<NewRouteMatcher>
) : HttpHandler {
    override fun invoke(request: Request) = routes
        .map { it.match(request) }
        .sortedBy(HttpMatchResult::priority)
        .first().handler(request)

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(filter: Filter) = copy(routes = routes.map { it.withFilter(filter) })

    fun withPredicate(predicate: Predicate) =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString() = routes.sortedBy(NewRouteMatcher::toString).joinToString("\n")
}

interface NewRouteMatcher {
    fun match(request: Request): HttpMatchResult
    fun withBasePath(prefix: String): NewRouteMatcher
    fun withPredicate(other: Predicate): NewRouteMatcher
    fun withFilter(new: Filter): NewRouteMatcher
}

data class TemplatedHttpRoute(
    private val uriTemplate: UriTemplate,
    private val handler: HttpHandler,
    private val predicate: Predicate = Any,
    private val filter: Filter = Filter.NoOp
) : NewRouteMatcher {
    init {
        require(handler !is RoutedHttpHandler)
    }

    override fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = predicate(request)) {
            is Matched -> HttpMatchResult(0, filter.then(AddUriTemplate(uriTemplate).then(handler)))
            is NotMatched -> HttpMatchResult(1, filter.then { _: Request -> Response(result.status) })
        }

        else -> HttpMatchResult(2,  filter.then { _: Request -> Response(NOT_FOUND) })
    }

    override fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    override fun withPredicate(other: Predicate) = copy(predicate = predicate.and(other))
    override fun withFilter(new: Filter): NewRouteMatcher = copy(filter = new.then(this.filter))

    override fun toString() = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = Filter { next ->
        {
            RoutedResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}

data class HttpMatchResult(val priority: Int, val handler: HttpHandler)

data class HttpPathMethod(val path: String, val method: Method) {
    infix fun to(handler: HttpHandler) = when (handler) {
        is RoutedHttpHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutedHttpHandler(listOf(TemplatedHttpRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}
