package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.websocket.NoOp
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus
import org.http4k.websocket.then

fun websockets(vararg list: RoutingWsHandler) = websockets(list.toList())

fun websockets(routers: List<RoutingWsHandler>) = RoutingWsHandler(routers.flatMap { it.routes })

fun websockets(ws: WsConsumer): WsHandler = { WsResponse(ws) }

class RoutedWsResponse(
    val delegate: WsResponse,
    override val xUriTemplate: UriTemplate,
) : WsResponse by delegate, RoutedMessage {

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun withConsumer(consumer: WsConsumer): WsResponse =
        RoutedWsResponse(delegate.withConsumer(consumer), xUriTemplate)

    override fun withSubprotocol(subprotocol: String?): WsResponse =
        RoutedWsResponse(delegate.withSubprotocol(subprotocol), xUriTemplate)
}

class RoutingWsHandler(
    routes: List<RouteMatcher<WsResponse, WsFilter>>
) : RoutingHandler<WsResponse, WsFilter, RoutingWsHandler>(
    routes,
    WsResponse { it.close(WsStatus.REFUSE) },
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
            is RoutingResult.Matched -> RoutingMatchResult(0, AddUriTemplate(uriTemplate).then(filter).then(handler))
            is RoutingResult.NotMatched -> RoutingMatchResult(
                1,
                filter.then { _: Request -> WsResponse { it.close(WsStatus.REFUSE) } })
        }

        else -> RoutingMatchResult(1, filter.then { _: Request -> WsResponse { it.close(WsStatus.REFUSE) } })
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
