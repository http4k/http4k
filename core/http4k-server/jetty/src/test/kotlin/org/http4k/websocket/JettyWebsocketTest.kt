package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty
import org.http4k.server.ServerConfig

class JettyWebsocketTest : WebsocketServerContract({ Jetty(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
