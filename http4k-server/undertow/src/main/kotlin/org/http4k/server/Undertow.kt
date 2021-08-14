package org.http4k.server

import io.undertow.Handlers.predicate
import io.undertow.Handlers.serverSentEvents
import io.undertow.Handlers.websocket
import io.undertow.Undertow
import io.undertow.UndertowOptions.ENABLE_HTTP2
import io.undertow.server.handlers.BlockingHandler
import org.http4k.core.HttpHandler
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress
import java.net.InetAddress

class Undertow(val port: Int = 8000, val enableHttp2: Boolean, val address: InetAddress?) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, false, null)

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        val httpHandler = http?.let(::Http4kUndertowHttpHandler)?.let(::BlockingHandler)
        val wsCallback = ws?.let { websocket(Http4kWebSocketCallback(it)) }
        val sseCallback = sse?.let { serverSentEvents(Http4kSseCallback(sse)) }

        val handlerWithWs = when {
            httpHandler != null && wsCallback != null -> predicate(requiresWebSocketUpgrade(), wsCallback, httpHandler)
            wsCallback != null -> wsCallback
            else -> httpHandler
        }

        val handlerWithSse = sseCallback
            ?.let { predicate(hasEventStreamContentType(), sseCallback, handlerWithWs) }
            ?: handlerWithWs

        return object : Http4kServer {
            val ipString = when (address) {
                null -> "0.0.0.0"
                else -> address.hostAddress
            }

            val server = Undertow.builder()
                .addHttpListener(port, ipString)
                .setServerOption(ENABLE_HTTP2, enableHttp2)
                .setHandler(handlerWithSse).build()

            override fun start() = apply { server.start() }

            override fun stop() = apply { server.stop() }

            override fun port(): Int = when {
                port > 0 -> port
                else -> (server.listenerInfo[0].address as InetSocketAddress).port
            }
        }
    }
}
