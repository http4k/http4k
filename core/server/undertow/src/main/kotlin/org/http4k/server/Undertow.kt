package org.http4k.server

import io.undertow.Handlers.predicate
import io.undertow.Handlers.websocket
import io.undertow.Undertow
import io.undertow.UndertowOptions.ENABLE_HTTP2
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.handlers.GracefulShutdownHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress

class Undertow(
    val port: Int = 8000,
    val enableHttp2: Boolean,
    override val stopMode: StopMode = Immediate
) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, false)
    constructor(port: Int = 8000, enableHttp2: Boolean) : this(port, enableHttp2, Immediate)

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        val httpHandler =
            (http ?: { Response(NOT_FOUND) }).let(::Http4kUndertowHttpHandler).let(::BlockingHandler).let { handler ->
                when (stopMode) {
                    is Graceful -> GracefulShutdownHandler(handler)
                    else -> handler
                }
            }
        val wsCallback = ws?.let { websocket(Http4kWebSocketCallback(it)) }

        val handlerWithWs = predicate(requiresWebSocketUpgrade(), wsCallback, httpHandler)

        val handlerWithSse = sse?.let { Http4kUndertowSseFallbackHandler(sse, handlerWithWs).let(::BlockingHandler) }

        return object : Http4kServer {
            val server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setServerOption(ENABLE_HTTP2, enableHttp2)
                .setWorkerThreads(32 * Runtime.getRuntime().availableProcessors())
                .setHandler(handlerWithSse ?: handlerWithWs).build()

            override fun start() = apply { server.start() }

            override fun stop() = apply {
                (httpHandler as? GracefulShutdownHandler)?.apply {
                    shutdown()
                    awaitShutdown((stopMode as Graceful).timeout.toMillis())
                }
                server.stop()
            }

            override fun port(): Int = when {
                port > 0 -> port
                else -> (server.listenerInfo[0].address as InetSocketAddress).port
            }
        }
    }
}
