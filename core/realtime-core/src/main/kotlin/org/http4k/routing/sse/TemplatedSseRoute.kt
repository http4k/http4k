package org.http4k.routing.sse

import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.routing.All
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedSseResponse
import org.http4k.routing.Router
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.http4k.routing.and
import org.http4k.sse.NoOp
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.sse.then

data class TemplatedSseRoute(
    private val uriTemplate: UriTemplate,
    private val handler: SseHandler,
    private val router: Router = All,
    private val filter: SseFilter = SseFilter.NoOp
) {
    init {
        require(handler !is RoutingSseHandler)
    }

    internal fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (val result = router(request)) {
            is Matched -> SseMatchResult(0, AddUriTemplate(uriTemplate).then(filter).then(handler))
            is NotMatched -> SseMatchResult(1, filter.then { _: Request -> SseResponse(result.status, handled = false) { it.close() } })
        }

        else -> SseMatchResult(2, filter.then { _: Request -> SseResponse(NOT_FOUND, handled = false) { it.close() } })
    }

    fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    fun withFilter(new: SseFilter) = copy(filter = new.then(filter))

    fun withRouter(other: Router) = copy(router = router.and(other))

    override fun toString() = "template=$uriTemplate AND ${router.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = SseFilter { next ->
        {
            RoutedSseResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}
