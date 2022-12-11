package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.websocket.core.WebSocketComponents
import org.eclipse.jetty.websocket.core.server.WebSocketUpgradeHandler
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.time.Duration

class Jetty(private val port: Int, override val stopMode: StopMode, private val server: Server) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Graceful(Duration.ofSeconds(5)))
    constructor(port: Int = 8000, stopMode: StopMode) : this(port, stopMode, http(port))
    constructor(port: Int = 8000, server: Server) : this(port, StopMode.Graceful(Duration.ofSeconds(5)), server)
    constructor(port: Int, vararg inConnectors: ConnectorBuilder) : this(port, StopMode.Graceful(Duration.ofSeconds(5)), *inConnectors)
    constructor(port: Int, stopMode: StopMode, vararg inConnectors: ConnectorBuilder) : this(port, stopMode, Server().apply {
        inConnectors.forEach { addConnector(it(this)) }
    })

    init {
        when (stopMode) {
            is StopMode.Graceful -> {
                server.apply {
                    stopTimeout = stopMode.timeout.toMillis()
                }
            }

            is StopMode.Immediate -> throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        if (sse != null) throw UnsupportedOperationException("Jetty does not support sse")
        http?.let { server.insertHandler(http.toJettyHandler(stopMode is StopMode.Graceful)) }
        ws?.let {
            server.insertHandler(
                WebSocketUpgradeHandler(
                    WebSocketComponents()).apply {
                    addMapping("/*", it.toJettyNegotiator())
                })
        }

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop(): Http4kServer = apply { server.stop() }

            override fun port(): Int = if (port > 0) port else server.uri.port
        }
    }
}
