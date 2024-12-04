package org.http4k.routing.websocket

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.All
import org.http4k.routing.RouteMatcher
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedWsResponse
import org.http4k.routing.Router
import org.http4k.routing.RoutingHandler
import org.http4k.routing.RoutingMatchResult
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.http4k.routing.and
import org.http4k.routing.asRouter
import org.http4k.websocket.NoOp
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE
import org.http4k.websocket.then

class RoutingWsHandler(
    routes: List<RouteMatcher<WsResponse, WsFilter>>
) : RoutingHandler<WsResponse, WsFilter, RoutingWsHandler>(
    routes,
    WsResponse { it.close(REFUSE) },
    ::RoutingWsHandler
)

data class TemplatedWsRoute(
    private val uriTemplate: UriTemplate,
    private val handler: WsHandler,
    private val router: Router = All,
    private val filter: WsFilter = WsFilter.NoOp
) : RouteMatcher<WsResponse, WsFilter> {
    init {
        require(handler !is RoutingWsHandler)
    }

    override fun match(request: Request) = when {
        uriTemplate.matches(request.uri.path) -> when (router(request)) {
            is Matched -> RoutingMatchResult(0, AddUriTemplate(uriTemplate).then(filter).then(handler))
            is NotMatched -> RoutingMatchResult(1, filter.then { _: Request -> WsResponse { it.close(REFUSE) } })
        }

        else -> RoutingMatchResult(1, filter.then { _: Request -> WsResponse { it.close(REFUSE) } })
    }

    override fun withBasePath(prefix: String) = copy(uriTemplate = UriTemplate.from("$prefix/${uriTemplate}"))

    override fun withRouter(other: Router) = copy(router = router.and(other))

    override fun withFilter(new: WsFilter) = copy(filter = new.then(filter))

    override fun toString() = "template=$uriTemplate AND ${router.description}"

    private fun AddUriTemplate(uriTemplate: UriTemplate) = WsFilter { next ->
        {
            RoutedWsResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate)
        }
    }
}

data class WsPathMethod(val path: String, val method: Method) {
    infix fun to(handler: WsHandler) = when (handler) {
        is RoutingWsHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
        else -> RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(path), handler, method.asRouter())))
    }
}
