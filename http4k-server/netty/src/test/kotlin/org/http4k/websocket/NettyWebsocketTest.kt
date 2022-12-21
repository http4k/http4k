package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Netty
import org.http4k.server.ServerConfig.StopMode.Immediate

class NettyWebsocketTest : WebsocketServerContract({ Netty(it, Immediate) }, JavaHttpClient())
