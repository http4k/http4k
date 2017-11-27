package org.http4k.server

import org.http4k.websocket.WebSocketClient
import org.http4k.websocket.WsHandler

fun WsHandler.client(): WebSocketClient = WebSocketClient(this)

