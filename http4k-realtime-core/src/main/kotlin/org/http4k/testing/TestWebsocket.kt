package org.http4k.testing

import org.http4k.core.Request
import org.http4k.server.PolyHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus

fun WsHandler.testWebsocket(request: Request): Websocket = object: PushPullAdaptingWebSocket(request) {
    init {
        invoke(request)(this)
    }

    override fun send(message: WsMessage) = triggerMessage(message)

    override fun close(status: WsStatus) = triggerClose(status)

}
fun PolyHandler.testWebsocket(request: Request): Websocket = ws?.testWebsocket(request) ?: error("No WS handler set.")
