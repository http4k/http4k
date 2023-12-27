package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11

class Jetty11WebsocketTest : WebsocketServerContract(::Jetty11, JavaHttpClient())
