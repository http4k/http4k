package org.http4k.routing.sse.experimental

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.http4k.routing.RoutedRequest
import org.http4k.routing.experimental.Any
import org.http4k.routing.experimental.Predicate
import org.http4k.routing.experimental.PredicateResult
import org.http4k.routing.experimental.and
import org.http4k.routing.experimental.asPredicate
import org.http4k.routing.sse.SseMatchResult
import org.http4k.sse.NoOp
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

data class RoutedSseHandler(
    val routes: List<TemplatedSseRoute>,
    private val filter: SseFilter = SseFilter.NoOp
) : SseHandler {
    override fun invoke(request: Request) = filter.then(routes
        .map { it.match(request) }
        .sortedBy(SseMatchResult::priority)
        .first().handler)(request)

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(filter: SseFilter) = copy(filter = filter.then(filter))

    fun withPredicate(predicate: Predicate) =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString() = routes.sortedBy(TemplatedSseRoute::toString).joinToString("\n")
}

data class SsePathMethod(val path: String, val method: Method) {
    infix fun to(handler: SseHandler) = when (handler) {
        is RoutedSseHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutedSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}


data class TemplatedSseRoute(
    private val uriTemplate: UriTemplate,
    private val handler: SseHandler,
    private val predicate: Predicate = Any
) {
    init {
        require(handler !is RoutedSseHandler)
    }

    internal fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = predicate(request)) {
            is PredicateResult.Matched -> SseMatchResult(0, AddUriTemplate(uriTemplate).then(handler))
            is PredicateResult.NotMatched -> SseMatchResult(1) { _: Request -> SseResponse(result.status) { it.close() } }
        }

        else -> SseMatchResult(2) { _: Request -> SseResponse(Status.NOT_FOUND) { it.close() } }
    }

    fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withPredicate(other: Predicate) = copy(predicate = predicate.and(other))

    override fun toString() = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = SseFilter { next ->
        {
            next(RoutedRequest(it, uriTemplate))
        }
    }
}
