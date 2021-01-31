package org.http4k.server

import io.undertow.server.HttpServerExchange
import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedBinaryMessage
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.core.WebSockets.sendBinary
import io.undertow.websockets.core.WebSockets.sendClose
import io.undertow.websockets.core.WebSockets.sendText
import io.undertow.websockets.spi.WebSocketHttpExchange
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus

class WebSocketUndertowCallback(private val ws: WsHandler) : WebSocketConnectionCallback {
    private var socket: PushPullAdaptingWebSocket? = null

    override fun onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel) {
        val upgradeRequest = exchange.asRequest()
        ws(upgradeRequest)?.also {
            socket = object : PushPullAdaptingWebSocket(upgradeRequest) {
                override fun send(message: WsMessage) =
                    if (message.body is StreamBody) sendBinary(message.body.payload, channel, null)
                    else sendText(message.bodyString(), channel, null)

                override fun close(status: WsStatus) {
                    println("sending close to client")
                    sendClose(status.code, status.description, channel, null)
                }
            }

            channel.addCloseTask {
                socket?.triggerClose(WsStatus(it.closeCode, it.closeReason ?: "unknown"))
            }

            channel.receiveSetter.set(object : AbstractReceiveListener() {
                override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) {
                    println("onFullTextMessage")
                    socket?.triggerMessage(WsMessage(Body(message.data)))
                }

                override fun onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage) {
                    println("onFullBinaryMessage")
                    socket?.let { s ->
                        message.data.resource.forEach { s.triggerMessage(WsMessage(Body(it))) }
                    }
                }

                override fun onError(channel: WebSocketChannel, error: Throwable) {
                    println("on error")
                    socket?.triggerError(error)
                }
            })
            channel.resumeReceives()
            socket?.apply(it)
        }
    }
}

private fun WebSocketHttpExchange.asRequest() = Request(GET, requestURI)
    .headers(requestHeaders.toList().flatMap { h -> h.second.map { h.first to it } })

fun requiresWebSocketUpgrade(): (HttpServerExchange) -> Boolean = {
    (it.requestHeaders["Connection"]?.any { it.equals("upgrade", true) } ?: false) &&
        (it.requestHeaders["Upgrade"]?.any { it.equals("websocket", true) } ?: false)
}
