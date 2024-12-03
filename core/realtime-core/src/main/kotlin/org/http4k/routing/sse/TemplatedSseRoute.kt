package org.http4k.routing.sse

import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.http4k.routing.All
import org.http4k.routing.Predicate
import org.http4k.routing.PredicateResult.Matched
import org.http4k.routing.PredicateResult.NotMatched
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedSseResponse
import org.http4k.routing.and
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

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
            is Matched -> SseMatchResult(0, AddUriTemplate(uriTemplate).then(handler))
            is NotMatched -> SseMatchResult(1) { _: Request -> SseResponse(result.status) { it.close() } }
        }

        else -> SseMatchResult(2) { _: Request -> SseResponse(Status.NOT_FOUND, handled = false) { it.close() } }
    }

    fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withPredicate(other: Predicate) = copy(predicate = predicate.and(other))

    override fun toString() = "template=$uriTemplate AND ${predicate.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = SseFilter { next ->
        {
            RoutedSseResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}
