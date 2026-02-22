package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.routing.RoutedMessage.Companion.X_URI_TEMPLATE
import org.http4k.routing.RoutingResult.Matched
import org.http4k.routing.RoutingResult.NotMatched
import org.http4k.websocket.NoOp
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus.Companion.NEVER_CONNECTED
import org.http4k.websocket.WsStatus.Companion.REFUSE
import org.http4k.websocket.then

fun websockets(vararg list: RoutingWsHandler) = websockets(list.toList())

fun websockets(routers: List<RoutingWsHandler>) = RoutingWsHandler(routers.flatMap { it.routes })

fun websockets(ws: WsConsumer): WsHandler = { WsResponse(ws) }

class WsResponseWithContext(
    val delegate: WsResponse,
    val context: Map<String, Any> = emptyMap()
) : WsResponse by delegate, RoutedMessage {

    constructor(delegate: WsResponse, uriTemplate: UriTemplate) : this(
        if (delegate is WsResponseWithContext) delegate.delegate else delegate,
        if (delegate is WsResponseWithContext) delegate.context + (X_URI_TEMPLATE to uriTemplate)
        else mapOf(X_URI_TEMPLATE to uriTemplate)
    )

    override val xUriTemplate: UriTemplate
        get() {
            return context[X_URI_TEMPLATE] as? UriTemplate
                ?: throw IllegalStateException("Message was not routed, so no uri-template present")
        }

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun withConsumer(consumer: WsConsumer): WsResponse =
        WsResponseWithContext(delegate.withConsumer(consumer), xUriTemplate)

    override fun withSubprotocol(subprotocol: String?): WsResponse =
        WsResponseWithContext(delegate.withSubprotocol(subprotocol), xUriTemplate)
}

class RoutingWsHandler(
    routes: List<RouteMatcher<WsResponse, WsFilter>>
) : RoutingHandler<WsResponse, WsFilter, RoutingWsHandler>(
    routes,
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
    addUriTemplateFilter = { next -> { WsResponseWithContext(next(RequestWithContext(it, uriTemplate)), uriTemplate) } }
) {
    override fun withBasePath(prefix: String) = TemplatedWsRoute(uriTemplate.prefixed(prefix), handler, router, filter)

    override fun withFilter(new: WsFilter) = TemplatedWsRoute(uriTemplate, handler, router, new.then(filter))

    override fun withRouter(other: Router) = TemplatedWsRoute(uriTemplate, handler, router.and(other), filter)
}

infix fun PathMethod.to(handler: WsHandler) = when (handler) {
    is RoutingWsHandler -> handler.withRouter(method.asRouter()).withBasePath(path)
    else -> RoutingWsHandler(listOf(TemplatedWsRoute(UriTemplate.from(path), handler, method.asRouter())))
}

data class SimpleWsRouteMatcher(
    private val router: Router,
    private val handler: WsHandler,
    private val filter: WsFilter = WsFilter.NoOp
) : RouteMatcher<WsResponse, WsFilter> {

    override fun match(request: Request) = when (val result = router(request)) {
        is Matched -> RoutingMatch(0, result.description, filter.then(handler))
        is NotMatched -> RoutingMatch(1, result.description, filter.then { _: Request -> WsResponse { it.close(
            NEVER_CONNECTED)} })
    }

    override fun withBasePath(prefix: String): RouteMatcher<WsResponse, WsFilter> =
        TemplatedWsRoute(UriTemplate.from(prefix), handler, router, filter)

    override fun withRouter(other: Router): RouteMatcher<WsResponse, WsFilter> = copy(router = router.and(other))

    override fun withFilter(new: WsFilter): RouteMatcher<WsResponse, WsFilter> = copy(filter = new.then(filter))

    override fun toString(): String = router.toString()
}
