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
import java.io.IOException

class Http4kWebSocketCallback(private val ws: WsHandler) : WebSocketConnectionCallback {

    override fun onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel) {
        val upgradeRequest = exchange.asRequest()

        val socket = object : PushPullAdaptingWebSocket(upgradeRequest) {
            override fun send(message: WsMessage) =
                if (message.body is StreamBody) sendBinary(message.body.payload, channel, null)
                else sendText(message.bodyString(), channel, null)

            override fun close(status: WsStatus) {
                sendClose(status.code, status.description, channel, null)
            }
        }.apply(ws(upgradeRequest))

        channel.addCloseTask {
            socket.triggerClose(WsStatus(it.closeCode, it.closeReason ?: "unknown"))
        }

        channel.receiveSetter.set(object : AbstractReceiveListener() {
            override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) {
                try {
                    socket.triggerMessage(WsMessage(Body(message.data)))
                } catch (e: IOException) {
                    throw e
                } catch (e: Exception) {
                    socket.triggerError(e)
                    throw e
                }
            }

            override fun onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage) =
                message.data.resource.forEach { socket.triggerMessage(WsMessage(Body(it))) }

            override fun onError(channel: WebSocketChannel, error: Throwable) = socket.triggerError(error)
        })
        channel.resumeReceives()
    }
}

private fun WebSocketHttpExchange.asRequest() = Request(GET, requestURI)
    .headers(requestHeaders.toList().flatMap { h -> h.second.map { h.first to it } })

fun requiresWebSocketUpgrade(): (HttpServerExchange) -> Boolean = { httpServerExchange ->
    val containsValidConnectionHeader = httpServerExchange.requestHeaders["Connection"]
        ?.any { headerValue ->
            headerValue.split(",")
                .map { it.trim().lowercase() }
                .contains("upgrade")
        } ?: false

    val containsValidUpgradeHeader = httpServerExchange.requestHeaders["Upgrade"]
        ?.any { it.equals("websocket", true) } ?: false

    containsValidConnectionHeader && containsValidUpgradeHeader
}
