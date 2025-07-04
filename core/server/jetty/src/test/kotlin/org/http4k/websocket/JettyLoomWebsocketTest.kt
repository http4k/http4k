package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.JettyLoom
import org.http4k.server.ServerConfig
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class JettyLoomWebsocketTest : WebsocketServerContract({ JettyLoom(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
