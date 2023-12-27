package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11Loom

class Jetty11LoomWebsocketTest : WebsocketServerContract(::Jetty11Loom, JavaHttpClient())
