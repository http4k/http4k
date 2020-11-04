package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Netty

class NettyWebsocketTest : WebsocketServerContract(::Netty, JavaHttpClient())
