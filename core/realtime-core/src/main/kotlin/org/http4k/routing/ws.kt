package org.http4k.routing

import org.http4k.core.Method
import org.http4k.core.UriTemplate
import org.http4k.websocket.NoOp
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.REFUSE
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
    WsResponse { it.close(REFUSE) },
    ::RoutingWsHandler
)

class TemplatedWsRoute(
    uriTemplate: UriTemplate, handler: WsHandler, router: Router = All, filter: WsFilter = WsFilter.NoOp
) : TemplatedRoute<WsResponse, WsFilter, TemplatedWsRoute>(
    uriTemplate = uriTemplate,
    handler = handler,
    router = router,
    filter = filter,
    responseFor = { WsResponse { it.close(REFUSE) } },
    addUriTemplateFilter = { next -> { RoutedWsResponse(next(RoutedRequest(it, uriTemplate)), uriTemplate) } }
) {
    override fun withBasePath(prefix: String) = TemplatedWsRoute(uriTemplate.prefixed(prefix), handler, router, filter)

    override fun withFilter(new: WsFilter) = TemplatedWsRoute(uriTemplate, handler, router, new.then(filter))

    override fun withRouter(other: Router) = TemplatedWsRoute(uriTemplate, handler, router.and(other), filter)
}

data class WsPathMethod(val path: String, val method: Method) {
    infix fun to(handler: WsHandler) = when (handler) {
        is RoutingWsHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
        else -> RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(path), handler, method.asRouter())))
    }
}
