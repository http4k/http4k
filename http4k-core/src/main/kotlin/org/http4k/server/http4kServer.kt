package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler

interface Http4kServer : AutoCloseable {
    fun start(): Http4kServer
    fun stop(): Http4kServer
    fun block() = Thread.currentThread().join()
    override fun close() {
        stop()
    }

    fun port(): Int
}

/**
 * Standard interface for creating a configured WebServer
 */
fun interface ServerConfig {
    fun toServer(httpHandler: HttpHandler): Http4kServer
}

/**
 * Standard interface for creating a configured WebServer which supports Websockets
 */
interface WsServerConfig : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = toServer(httpHandler, null)
    fun toWsServer(wsHandler: WsHandler): Http4kServer = toServer(null, wsHandler)
    fun toServer(httpHandler: HttpHandler? = null, wsHandler: WsHandler? = null): Http4kServer
}

/**
 * Standard interface for creating a configured WebServer which supports SSE
 */
interface SseServerConfig : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = toServer(httpHandler, null)
    fun toSseServer(sseHandler: SseHandler): Http4kServer = toServer(null, sseHandler)
    fun toServer(httpHandler: HttpHandler? = null, wsHandler: WsHandler? = null): Http4kServer
}

@JvmName("consumerAsServer")
fun SseConsumer.asServer(config: SseServerConfig): Http4kServer = { _: Request -> this@asServer }.asServer(config)

@JvmName("consumerAsServer")
fun WsConsumer.asServer(config: WsServerConfig): Http4kServer = { _: Request -> this@asServer }.asServer(config)

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)
fun SseHandler.asServer(config: SseServerConfig): Http4kServer = config.toSseServer(this)
fun WsHandler.asServer(config: WsServerConfig): Http4kServer = config.toWsServer(this)
fun org.http4k.websocket.PolyHandler.asServer(config: WsServerConfig): Http4kServer = config.toServer(http, ws)
fun org.http4k.sse.PolyHandler.asServer(config: SseServerConfig): Http4kServer = config.toServer(http, sse)
