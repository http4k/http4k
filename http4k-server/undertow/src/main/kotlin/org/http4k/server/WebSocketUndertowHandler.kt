package org.http4k.server

import io.undertow.Handlers.websocket
import io.undertow.websockets.core.AbstractReceiveListener
import io.undertow.websockets.core.BufferedBinaryMessage
import io.undertow.websockets.core.BufferedTextMessage
import io.undertow.websockets.core.CloseMessage
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

fun WebSocketUndertowHandler(ws: WsHandler) =
    websocket { exchange, channel ->
        val upgradeRequest = exchange.asRequest()
        ws(upgradeRequest)?.also {
            val socket = object : PushPullAdaptingWebSocket(upgradeRequest) {
                override fun send(message: WsMessage) =
                    if (message.body is StreamBody) sendBinary(message.body.payload, channel, null)
                    else sendText(message.bodyString(), channel, null)

                override fun close(status: WsStatus) = sendClose(status.code, status.description, channel, null)
            }.apply(it)

            channel.receiveSetter.set(object : AbstractReceiveListener() {
                override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) =
                    socket.triggerMessage(WsMessage(Body(message.data)))

                override fun onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage) =
                    message.data.resource.forEach { socket.triggerMessage(WsMessage(Body(it))) }

                override fun onCloseMessage(cm: CloseMessage, channel: WebSocketChannel) =
                    socket.triggerClose(WsStatus(cm.code, cm.reason))

                override fun onError(channel: WebSocketChannel, error: Throwable) = socket.triggerError(error)
            })
            channel.resumeReceives()
        }
    }

private fun WebSocketHttpExchange.asRequest() = Request(GET, requestURI)
    .headers(requestHeaders.toList().flatMap { h -> h.second.map { h.first to it } })
