package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Netty
import org.http4k.server.WebSocketServerHandler
import org.junit.jupiter.api.Test

class NettyWebsocketTest : WebsocketServerContract(::Netty, JavaHttpClient()) {

    @Test
    fun `override websocket config`() {
        Netty(8000, createWebsocketHandler = { ws ->
            WebSocketServerHandler(ws) {
                it.maxFramePayloadLength(30_000)
            }
        })
    }
}
