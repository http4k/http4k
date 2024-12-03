package org.http4k.routing

import org.http4k.core.UriTemplate
import org.http4k.routing.websocket.RoutingWsHandler
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse

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
