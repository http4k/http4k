package org.http4k.routing

import org.http4k.core.Method
import org.http4k.routing.ws.RoutingWsHandler
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse

fun websockets(vararg list: Pair<Method, WsHandler>) =
    websockets(*list.map { "" bind it.first to it.second }.toTypedArray())

fun websockets(vararg list: RoutingWsHandler) = websockets(list.toList())

fun websockets(routers: List<RoutingWsHandler>) = RoutingWsHandler(routers.flatMap { it.routes })

fun websockets(ws: WsConsumer): WsHandler = { WsResponse(ws) }
