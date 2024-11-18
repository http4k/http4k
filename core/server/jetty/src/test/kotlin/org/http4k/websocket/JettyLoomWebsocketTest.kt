package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.JettyLoom
import org.http4k.server.ServerConfig

class JettyLoomWebsocketTest : WebsocketServerContract({ JettyLoom(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
