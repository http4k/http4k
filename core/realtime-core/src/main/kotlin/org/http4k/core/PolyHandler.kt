package org.http4k.core

import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

/**
 * A PolyHandler represents the combined routing logic of an multiple protocol handlers
 */
data class PolyHandler @JvmOverloads constructor(
    val http: HttpHandler? = null,
    val ws: WsHandler? = null,
    val sse: SseHandler? = null
)
