package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.server.PolyHandler
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

fun routes(fn: PolyHandlerBuilder.() -> Unit) = PolyHandlerBuilder().apply(fn)
    .run { PolyHandler(http, ws, sse) }

class PolyHandlerBuilder internal constructor(
    var http: HttpHandler? = null,
    var ws: WsHandler? = null,
    var sse: SseHandler? = null
)
