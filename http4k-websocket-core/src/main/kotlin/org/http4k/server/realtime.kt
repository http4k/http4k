package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler

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

@JvmName("sseHandlerAsServer")
fun SseHandler.asServer(config: PolyServerConfig): Http4kServer = config.toSseServer(this)

@JvmName("wsHandlerAsServer")
fun WsHandler.asServer(config: PolyServerConfig): Http4kServer = config.toWsServer(this)

fun org.http4k.server.PolyHandler.asServer(config: PolyServerConfig): Http4kServer = config.toServer(http, ws, sse)
