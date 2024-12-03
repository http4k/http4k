package org.http4k.routing.websocket

import org.http4k.websocket.WsHandler

internal data class WsMatchResult(val priority: Int, val handler: WsHandler)
