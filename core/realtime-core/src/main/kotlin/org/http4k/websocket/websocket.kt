package org.http4k.websocket

import org.http4k.core.Request
import org.http4k.routing.websocket.RoutingWsHandler
import org.http4k.websocket.WsStatus.Companion.NORMAL

/**
 * Represents a connected Websocket instance, and can be passed around an application. This is configured
 * to react to events on the WS event stream by attaching listeners.
 */
interface Websocket {
    fun send(message: WsMessage)
    fun close(status: WsStatus = NORMAL)
    fun onError(fn: (Throwable) -> Unit)
    fun onClose(fn: (WsStatus) -> Unit)
    fun onMessage(fn: (WsMessage) -> Unit)
}

typealias WsConsumer = (Websocket) -> Unit

typealias WsHandler = (Request) -> WsResponse

fun interface WsFilter : (WsHandler) -> WsHandler {
    companion object
}

val WsFilter.Companion.NoOp: WsFilter get() = WsFilter { next -> { next(it) } }

fun WsFilter.then(next: WsFilter): WsFilter = WsFilter { this(next(it)) }

fun WsFilter.then(next: WsHandler): WsHandler = { this(next)(it) }

fun WsFilter.then(routingWsHandler: RoutingWsHandler): RoutingWsHandler = routingWsHandler.withFilter(this)
