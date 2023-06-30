package org.http4k.server

import org.http4k.core.Request
import org.http4k.sse.SseConsumer
import org.http4k.sse.SseHandler
import org.http4k.sse.SseResponse
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsResponse

@JvmName("wsConsumerAsServer")
fun WsConsumer.asServer(config: PolyServerConfig): Http4kServer = { _: Request -> WsResponse(this@asServer) }.asServer(config)

@JvmName("sseConsumerAsServer")
fun SseConsumer.asServer(config: PolyServerConfig): Http4kServer = { _: Request -> SseResponse(this@asServer) }.asServer(config)

@JvmName("sseHandlerAsServer")
fun SseHandler.asServer(config: PolyServerConfig): Http4kServer = config.toSseServer(this)

@JvmName("wsHandlerAsServer")
fun WsHandler.asServer(config: PolyServerConfig): Http4kServer = config.toWsServer(this)

fun PolyHandler.asServer(config: PolyServerConfig): Http4kServer = config.toServer(http, ws, sse)
