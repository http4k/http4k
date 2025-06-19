package org.http4k.client

import org.http4k.server.Jetty
import org.http4k.websocket.NonBlockingWebsocketClientContract
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class HelidonNonBlockingWebsocketClientTest: NonBlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websockets = HelidonWebsocketClient()
)
