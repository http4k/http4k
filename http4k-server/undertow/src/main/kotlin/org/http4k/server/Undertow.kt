package org.http4k.server

import io.undertow.Handlers.predicate
import io.undertow.Handlers.websocket
import io.undertow.Undertow
import io.undertow.UndertowOptions.ENABLE_HTTP2
import io.undertow.server.handlers.BlockingHandler
import org.http4k.core.HttpHandler
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress

data class Undertow(val port: Int = 8000, val enableHttp2: Boolean) : WsServerConfig {
    constructor(port: Int = 8000) : this(port, false)

    override fun toServer(httpHandler: HttpHandler?, wsHandler: WsHandler?): Http4kServer {
        val http = httpHandler?.let(::HttpUndertowHandler)?.let(::BlockingHandler)
        val ws = wsHandler?.let { websocket(Http4kWebSocketCallback(it)) }

        val handler = when {
            http != null && ws != null -> predicate(requiresWebSocketUpgrade(), ws, http)
            ws != null -> ws
            else -> http
        }

        return object : Http4kServer {
            val server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setServerOption(ENABLE_HTTP2, enableHttp2)
                .setHandler(handler).build()

            override fun start() = apply { server.start() }

            override fun stop() = apply { server.stop() }

            override fun port(): Int = when {
                port > 0 -> port
                else -> (server.listenerInfo[0].address as InetSocketAddress).port
            }
        }
    }
}
