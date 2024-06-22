package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11Loom
import org.http4k.testingStopMode

class Jetty11LoomWebsocketTest : WebsocketServerContract({ Jetty11Loom(it, testingStopMode) }, JavaHttpClient())
