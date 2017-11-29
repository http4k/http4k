package org.http4k.websocket

import org.http4k.client.ApacheClient
import org.http4k.server.Jetty

class JettyWebsocketTest : WebsocketServerContract(::Jetty, ApacheClient())