package org.http4k.routing.sse

import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.Router
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse

data class RoutingSseHandler(
    val routes: List<TemplatedSseRoute>,
) : SseHandler {
    override fun invoke(request: Request) = routes
        .map { it.match(request) }
        .sortedBy(SseMatchResult::priority)
        .firstOrNull()
        ?.handler?.invoke(request)
        ?: SseResponse(NOT_FOUND, handled = false) { it.close() }

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(new: SseFilter) = copy(routes = routes.map { it.withFilter(new) })

    fun withRouter(router: Router) =
        copy(routes = routes.map { it.withRouter(router) })

    override fun toString() = routes.sortedBy(TemplatedSseRoute::toString).joinToString("\n")
}

