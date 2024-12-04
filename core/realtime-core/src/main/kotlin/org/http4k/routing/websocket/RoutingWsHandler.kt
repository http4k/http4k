package org.http4k.routing.websocket

import org.http4k.core.Request
import org.http4k.routing.Router
import org.http4k.routing.RoutingMatchResult
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE

data class RoutingWsHandler(
    val routes: List<TemplatedWsRoute>,
) : WsHandler {
    override fun invoke(request: Request) = routes
        .map { it.match(request) }
        .sortedBy(RoutingMatchResult<WsResponse>::priority)
        .firstOrNull()
        ?.handler?.invoke(request)
        ?: WsResponse { it.close(REFUSE) }

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(new: WsFilter) = copy(routes = routes.map { it.withFilter(new) })

    fun withPredicate(router: Router) =
        copy(routes = routes.map { it.withPredicate(router) })

    override fun toString() = routes.sortedBy(TemplatedWsRoute::toString).joinToString("\n")
}
