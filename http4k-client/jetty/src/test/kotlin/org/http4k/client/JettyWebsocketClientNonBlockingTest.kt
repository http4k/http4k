package org.http4k.client

import org.http4k.server.Jetty
import org.http4k.websocket.NonBlockingWebsocketClientContract

class JettyWebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websocketFactory = { uri, headers, onError, onConnect ->
        JettyWebsocketClient.nonBlocking(uri, headers = headers, onError = onError, onConnect = onConnect)
    }
)
