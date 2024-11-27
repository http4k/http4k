package org.http4k.server.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.websocket.WebsocketServerContract

class JavaWebSocketTest : WebsocketServerContract({ JavaWebSocket(it, addShutdownHook = false) }, JavaHttpClient(), httpSupported = false)
