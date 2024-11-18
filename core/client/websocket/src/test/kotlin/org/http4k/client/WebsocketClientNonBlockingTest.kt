package org.http4k.client

import org.http4k.server.Jetty
import org.http4k.websocket.NonBlockingWebsocketClientContract

class WebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websocketFactory = { uri, headers, onError, onConnect ->
        WebsocketClient.nonBlocking(uri, headers, onError = onError, onConnect = onConnect)
    }
)
