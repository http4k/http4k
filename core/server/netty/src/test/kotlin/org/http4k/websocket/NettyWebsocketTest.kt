package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Netty
import org.http4k.server.defaultStopMode

class NettyWebsocketTest : WebsocketServerContract({ Netty(it, defaultStopMode) }, JavaHttpClient())
