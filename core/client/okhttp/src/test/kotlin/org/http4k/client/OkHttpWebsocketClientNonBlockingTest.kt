package org.http4k.client

import org.http4k.server.Undertow
import org.http4k.websocket.NonBlockingWebsocketClientContract

class OkHttpWebsocketClientNonBlockingTest : NonBlockingWebsocketClientContract(
    serverConfig = Undertow(0),
    websockets = OkHttpWebsocketClient()
)
