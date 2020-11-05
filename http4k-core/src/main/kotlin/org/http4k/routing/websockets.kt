package org.http4k.routing

import org.http4k.core.Request
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler

interface RoutingWsHandler : WsHandler {
    fun withBasePath(new: String): RoutingWsHandler
}

fun websockets(ws: WsConsumer): WsHandler = { ws }

fun websockets(vararg list: RoutingWsHandler): RoutingWsHandler = object : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = list.firstOrNull { it(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingWsHandler = websockets(*list.map { it.withBasePath(new) }.toTypedArray())
}
