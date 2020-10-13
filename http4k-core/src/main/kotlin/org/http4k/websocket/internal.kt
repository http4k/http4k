package org.http4k.websocket

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.websocket.WsStatus.Companion.NORMAL

abstract class PushPullAdaptingWebSocket(override val upgradeRequest: Request) : Websocket {

    private val errorHandlers: MutableList<(Throwable) -> Unit> = mutableListOf()
    private val closeHandlers: MutableList<(WsStatus) -> Unit> = mutableListOf()
    private val messageHandlers: MutableList<(WsMessage) -> Unit> = mutableListOf()

    fun triggerError(throwable: Throwable) = errorHandlers.forEach { it(throwable) }
    fun triggerClose(status: WsStatus = NORMAL) = closeHandlers.forEach { it(status) }
    fun triggerMessage(message: WsMessage) = messageHandlers.forEach { it(message) }

    override fun onError(fn: (Throwable) -> Unit) {
        errorHandlers.add(fn)
    }

    override fun onClose(fn: (WsStatus) -> Unit) {
        closeHandlers.add(fn)
    }

    override fun onMessage(fn: (WsMessage) -> Unit) {
        messageHandlers.add(fn)
    }
}

class Http4kWebSocketAdapter(private val innerSocket: PushPullAdaptingWebSocket) {
    fun onError(throwable: Throwable) = innerSocket.triggerError(throwable)
    fun onClose(status: WsStatus) = innerSocket.triggerClose(status)

    fun onMessage(body: Body) = innerSocket.triggerMessage(WsMessage(body))
}
