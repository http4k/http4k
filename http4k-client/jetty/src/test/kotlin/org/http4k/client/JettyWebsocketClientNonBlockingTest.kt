package org.http4k.client

import org.http4k.server.Jetty
import org.http4k.server.ServerConfig
import org.http4k.websocket.NonBlockingWebsocketClientContract

class JettyWebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0, ServerConfig.StopMode.Immediate),
    websocketFactory = { uri, headers, onError, onConnect ->
        JettyWebsocketClient.nonBlocking(uri, headers = headers, onError = onError, onConnect = onConnect)
    }
)
