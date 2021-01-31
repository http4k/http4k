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
    fun toServer(http: HttpHandler): Http4kServer
}

/**
 * Standard interface for creating a configured WebServer which supports Websockets
 */
interface PolyServerConfig : ServerConfig {
    override fun toServer(http: HttpHandler): Http4kServer = toServer(http, null)
    fun toWsServer(ws: WsHandler): Http4kServer = toServer(null, ws, null)
    fun toSseServer(sse: SseHandler): Http4kServer = toServer(null, null, sse)
    fun toServer(http: HttpHandler? = null, ws: WsHandler? = null, sse: SseHandler? = null): Http4kServer
}

@JvmName("wsConsumerAsServer")
fun WsConsumer.asServer(config: PolyServerConfig): Http4kServer = { _: Request -> this@asServer }.asServer(config)
@JvmName("sseConsumerAsServer")
fun SseConsumer.asServer(config: PolyServerConfig): Http4kServer = { _: Request -> this@asServer }.asServer(config)

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)
@JvmName("sseHandlerAsServer")
fun SseHandler.asServer(config: PolyServerConfig): Http4kServer = config.toSseServer(this)
@JvmName("wsHandlerAsServer")
fun WsHandler.asServer(config: PolyServerConfig): Http4kServer = config.toWsServer(this)

fun PolyHandler.asServer(config: PolyServerConfig): Http4kServer = config.toServer(http, ws, sse)
