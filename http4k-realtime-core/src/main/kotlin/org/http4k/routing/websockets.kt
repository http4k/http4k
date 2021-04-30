package org.http4k.routing

import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler

interface WsRouter {
    fun match(request: Request): WsRouterMatch
    fun withBasePath(new: String): WsRouter
    fun withFilter(new: WsFilter): WsRouter
}

interface RoutingWsHandler : WsHandler, WsRouter {
    override fun withBasePath(new: String): RoutingWsHandler
    override fun withFilter(new: WsFilter): RoutingWsHandler
}

infix fun String.bind(ws: WsConsumer): RoutingWsHandler = TemplateRoutingWsHandler(UriTemplate.from(this), ws)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

fun websockets(ws: WsConsumer): WsHandler = { ws }

fun websockets(vararg list: WsRouter): RoutingWsHandler = RouterWsHandler(list.toList())
