package org.http4k.routing.sse

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.http4k.routing.All
import org.http4k.routing.Predicate
import org.http4k.routing.PredicateResult
import org.http4k.routing.RoutedRequest
import org.http4k.routing.and
import org.http4k.routing.asPredicate
import org.http4k.sse.NoOp
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

data class RoutingSseHandler(
    val routes: List<TemplatedSseRoute>,
    private val filter: SseFilter = SseFilter.NoOp
) : SseHandler {
    override fun invoke(request: Request) = filter.then(routes
        .map { it.match(request) }
        .sortedBy(SseMatchResult::priority)
        .first().handler)(request)

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(new: SseFilter) = copy(filter = new.then(filter))

    fun withPredicate(predicate: Predicate) =
        copy(routes = routes.map { it.withPredicate(predicate) })

    override fun toString() = routes.sortedBy(TemplatedSseRoute::toString).joinToString("\n")
}

data class SsePathMethod(val path: String, val method: Method) {
    infix fun to(handler: SseHandler) = when (handler) {
        is RoutingSseHandler -> handler.withPredicate(method.asPredicate()).withBasePath(path)
        else -> RoutingSseHandler(listOf(TemplatedSseRoute(UriTemplate.from(path), handler, method.asPredicate())))
    }
}


data class TemplatedSseRoute(
    private val uriTemplate: UriTemplate,
    private val handler: SseHandler,
    private val predicate: Predicate = All
) {
    init {
        require(handler !is RoutingSseHandler)
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

internal data class SseMatchResult(val priority: Int, val handler: SseHandler)
