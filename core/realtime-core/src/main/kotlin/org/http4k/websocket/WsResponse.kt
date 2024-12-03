package org.http4k.websocket

data class WsResponse(val subprotocol: String? = null, val consumer: WsConsumer) : WsConsumer by consumer {
    constructor(consumer: WsConsumer) : this(null, consumer)
}
