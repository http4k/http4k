package org.http4k.websocket

import org.http4k.core.Status

abstract class PullPushAdaptingWebSocket : WebSocket {

    private val errorHandlers: MutableList<(Throwable) -> Unit> = mutableListOf()
    private val closeHandlers: MutableList<(Status) -> Unit> = mutableListOf()
    private val messageHandlers: MutableList<(WsMessage) -> Unit> = mutableListOf()

    fun triggerError(throwable: Throwable) = errorHandlers.forEach { it(throwable) }
    fun triggerClose(status: Status) = closeHandlers.forEach { it(status) }
    fun triggerMessage(message: WsMessage) = messageHandlers.forEach { it(message) }

    override fun onError(fn: (Throwable) -> Unit): PullPushAdaptingWebSocket {
        errorHandlers.add(fn)
        return this
    }

    override fun onClose(fn: (Status) -> Unit): PullPushAdaptingWebSocket {
        closeHandlers.add(fn)
        return this
    }

    override fun onMessage(fn: (WsMessage) -> Unit): PullPushAdaptingWebSocket {
        messageHandlers.add(fn)
        return this
    }
}