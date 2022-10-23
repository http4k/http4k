package org.http4k.client

import org.http4k.server.Undertow
import org.http4k.websocket.NonBlockingWebsocketClientContract

class WebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Undertow(0),
    websocketFactory = { uri, headers, onError, onConnect ->
        WebsocketClient.nonBlocking(uri, headers, onError = onError, onConnect = onConnect)
    }
)
