package org.http4k.testing

import org.http4k.core.Request
import org.http4k.server.PolyHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus

class TestWebsocket(response: WsResponse) : PushPullAdaptingWebSocket() {

    private val client = this
    private val server = object : PushPullAdaptingWebSocket() {
        init {
            response.consumer(this)
            onClose {
                client.triggerClose(it)
            }
        }

        override fun send(message: WsMessage) {
            client.triggerMessage(message)
        }

        override fun close(status: WsStatus) {
            client.triggerClose(status)
        }
    }

    override fun send(message: WsMessage) = server.triggerMessage(message)

    override fun close(status: WsStatus) = server.triggerClose(status)
}

fun WsHandler.testWebsocket(request: Request): Websocket = TestWebsocket(invoke(request))
