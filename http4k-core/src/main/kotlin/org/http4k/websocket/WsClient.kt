package org.http4k.websocket

import org.http4k.websocket.WsStatus.Companion.NORMAL

interface WsClient {
    fun received(): Sequence<WsMessage>
    fun close(status: WsStatus = NORMAL)
    fun send(message: WsMessage)
}
