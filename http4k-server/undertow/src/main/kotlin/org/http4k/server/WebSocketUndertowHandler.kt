package org.http4k.server

import io.undertow.Handlers.websocket
import io.undertow.websockets.spi.WebSocketHttpExchange
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.websocket.WsHandler
import io.undertow.websockets.core.BufferedTextMessage

import io.undertow.websockets.core.WebSocketChannel

import io.undertow.websockets.core.AbstractReceiveListener

import io.undertow.websockets.WebSocketConnectionCallback
import io.undertow.websockets.core.WebSockets.sendText

fun WebSocketUndertowHandler(ws: WsHandler) =
    websocket(WebSocketConnectionCallback { exchange, channel ->
        ws(exchange.asRequest())?.also {
            channel.receiveSetter.set(object : AbstractReceiveListener() {

                override fun onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage) {
                    sendText(message.data, channel, null)
                }
            })
            channel.resumeReceives()
        }
    })

private fun WebSocketHttpExchange.asRequest() = Request(GET, requestURI)
        .headers(requestHeaders.toList().flatMap { h -> h.second.map { h.first to it } })
