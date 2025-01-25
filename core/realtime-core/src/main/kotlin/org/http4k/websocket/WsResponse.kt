package org.http4k.websocket

interface WsResponse : WsConsumer {
    val subprotocol: String?
    val consumer: WsConsumer

    fun withSubprotocol(subprotocol: String?): WsResponse
    fun withConsumer(consumer: WsConsumer): WsResponse

    companion object {
        operator fun invoke(consumer: WsConsumer): WsResponse = MemoryWsResponse(null, consumer)
        operator fun invoke(subprotocol: String?, consumer: WsConsumer): WsResponse =
            MemoryWsResponse(subprotocol, consumer)
    }
}

internal data class MemoryWsResponse(override val subprotocol: String? = null, override val consumer: WsConsumer) :
    WsResponse,
    WsConsumer by consumer {
    override fun withSubprotocol(subprotocol: String?): WsResponse = copy(subprotocol = subprotocol)

    override fun withConsumer(consumer: WsConsumer): WsResponse = copy(consumer = consumer)
}
