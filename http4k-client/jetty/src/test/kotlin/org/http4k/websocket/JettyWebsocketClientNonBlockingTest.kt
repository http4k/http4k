package org.http4k.websocket

import org.http4k.client.JettyWebsocketClient
import org.http4k.server.Jetty

class JettyWebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websocketFactory = { uri, headers, onError, onConnect ->
        JettyWebsocketClient.nonBlockingWebsocket(uri, headers = headers, onError = onError, onConnect = onConnect)
    }
)
