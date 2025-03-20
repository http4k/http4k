package org.http4k.client

import org.http4k.server.Jetty
import org.http4k.server.ServerConfig
import org.http4k.websocket.NonBlockingWebsocketClientContract

class JettyWebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0, ServerConfig.StopMode.Immediate),
    websockets = JettyWebsocketClient()
)
