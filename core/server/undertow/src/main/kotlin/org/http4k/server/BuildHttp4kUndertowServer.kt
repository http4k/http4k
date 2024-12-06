package org.http4k.server

import io.undertow.Handlers.predicate
import io.undertow.Handlers.websocket
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.BlockingHandler
import io.undertow.server.handlers.GracefulShutdownHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.net.InetSocketAddress

fun Undertow.Builder.buildHttp4kUndertowServer(
    httpHandler: HttpHandler,
    stopMode: StopMode,
    port: Int
) = object : Http4kServer {
    val server = build()

    override fun start() = apply { server.start() }

    override fun stop() = apply {
        (httpHandler as? GracefulShutdownHandler)?.apply {
            shutdown()
            awaitShutdown((stopMode as StopMode.Graceful).timeout.toMillis())
        }
        server.stop()
    }

    override fun port(): Int = when {
        port > 0 -> port
        else -> (server.listenerInfo[0].address as InetSocketAddress).port
    }
}

fun buildUndertowHandlers(
    http: org.http4k.core.HttpHandler?,
    ws: WsHandler?,
    sse: SseHandler?,
    stopMode1: StopMode
): Pair<HttpHandler, HttpHandler> {
    val httpHandler =
        (http ?: { Response(NOT_FOUND) }).let(::Http4kUndertowHttpHandler).let(::BlockingHandler).let { handler ->
            when (stopMode1) {
                is Graceful -> GracefulShutdownHandler(handler)
                else -> handler
            }
        }
    val wsCallback = ws?.let { websocket(Http4kWebSocketCallback(it)) }
    val handlerWithWs = predicate(requiresWebSocketUpgrade(), wsCallback, httpHandler)
    val handlerWithSse = sse?.let { Http4kUndertowSseFallbackHandler(sse, handlerWithWs).let(::BlockingHandler) }

    return httpHandler to (handlerWithSse ?: handlerWithWs)
}

fun defaultUndertowBuilder(port: Int, httpHandler1: HttpHandler): Undertow.Builder =
    Undertow.builder()
        .addHttpListener(port, "0.0.0.0")
        .setWorkerThreads(32 * Runtime.getRuntime().availableProcessors())
        .setHandler(httpHandler1)
