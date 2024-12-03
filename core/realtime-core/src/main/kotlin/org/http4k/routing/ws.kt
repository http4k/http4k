package org.http4k.routing

import org.http4k.core.UriTemplate
import org.http4k.routing.websocket.RoutingWsHandler
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse

fun websockets(vararg list: RoutingWsHandler) = websockets(list.toList())

fun websockets(routers: List<RoutingWsHandler>) = RoutingWsHandler(routers.flatMap { it.routes })

fun websockets(ws: WsConsumer): WsHandler = { WsResponse(ws) }

data class RoutedWsResponse(
    val delegate: WsResponse,
    override val xUriTemplate: UriTemplate,
) : WsResponse by delegate, RoutedMessage
