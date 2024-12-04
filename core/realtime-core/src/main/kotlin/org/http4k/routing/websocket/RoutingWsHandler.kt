package org.http4k.routing.websocket

import org.http4k.core.Request
import org.http4k.routing.Router
import org.http4k.websocket.NoOp
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.then

data class RoutingWsHandler(
    val routes: List<TemplatedWsRoute>,
    private val filter: WsFilter = WsFilter.NoOp
) : WsHandler {
    override fun invoke(request: Request) = filter.then((routes
        .map { it.match(request) }
        .sortedBy(WsMatchResult::priority)
        .firstOrNull() ?: TemplatedWsRoute.notMachResult)
        .handler)(request)

    fun withBasePath(prefix: String) = copy(routes = routes.map { it.withBasePath(prefix) })

    fun withFilter(new: WsFilter) = copy(filter = new.then(filter))

    fun withPredicate(router: Router) =
        copy(routes = routes.map { it.withPredicate(router) })

    override fun toString() = routes.sortedBy(TemplatedWsRoute::toString).joinToString("\n")
}
