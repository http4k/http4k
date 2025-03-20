package org.http4k.client

import org.http4k.server.Jetty
import org.http4k.websocket.NonBlockingWebsocketClientContract

class HelidonNonBlockingWebsocketClientContract: NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websockets = HelidonWebsocketClient()
)
