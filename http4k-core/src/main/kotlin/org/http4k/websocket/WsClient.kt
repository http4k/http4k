package org.http4k.websocket

interface WsClient {
    fun received(): Sequence<WsMessage>
    fun close(status: WsStatus = WsStatus.NORMAL)
    fun send(message: WsMessage)
}