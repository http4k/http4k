package org.http4k.server

import org.http4k.client.JavaHttpClient
import org.http4k.websocket.WebsocketServerContract

class UndertowWebsocketTest : WebsocketServerContract(::Undertow, JavaHttpClient())
