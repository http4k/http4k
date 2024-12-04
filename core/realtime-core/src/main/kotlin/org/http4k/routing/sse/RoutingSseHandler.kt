package org.http4k.routing.sse

import org.http4k.core.Request
import org.http4k.routing.Router
import org.http4k.sse.NoOp
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
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

    fun withRouter(router: Router) =
        copy(routes = routes.map { it.withRouter(router) })

    override fun toString() = routes.sortedBy(TemplatedSseRoute::toString).joinToString("\n")
}

