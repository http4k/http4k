package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11
import org.http4k.testingStopMode
import org.junit.jupiter.api.Disabled

@Disabled("temporarily disabled")
class Jetty11WebsocketTest : WebsocketServerContract({ Jetty11(it, testingStopMode) }, JavaHttpClient())
