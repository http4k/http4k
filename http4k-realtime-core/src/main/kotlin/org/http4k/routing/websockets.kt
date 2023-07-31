package org.http4k.routing

import org.http4k.core.Request
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse

interface WsRouter {
    fun match(request: Request): WsRouterMatch
    fun withBasePath(new: String): WsRouter
    fun withFilter(new: WsFilter): WsRouter
}

interface RoutingWsHandler : WsHandler, WsRouter {
    override fun withBasePath(new: String): RoutingWsHandler
    override fun withFilter(new: WsFilter): RoutingWsHandler
}

fun websockets(ws: WsConsumer): WsHandler = { WsResponse(ws) }

fun websockets(vararg list: WsRouter): RoutingWsHandler = RouterWsHandler(list.toList())
