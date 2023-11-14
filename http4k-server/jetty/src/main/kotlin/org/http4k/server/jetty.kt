package org.http4k.server

import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

class Jetty(private val port: Int, override val stopMode: StopMode, private val server: Server) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, defaultStopMode)
    constructor(port: Int = 8000, stopMode: StopMode) : this(port, stopMode, http(port))
    constructor(port: Int = 8000, server: Server) : this(port, defaultStopMode, server)
    constructor(port: Int, vararg inConnectors: ConnectorBuilder) : this(port, defaultStopMode, *inConnectors)

    constructor(port: Int, stopMode: StopMode, vararg inConnectors: ConnectorBuilder) : this(
        port,
        stopMode,
        Server().apply { inConnectors.forEach { addConnector(it(this)) } })

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        server.handler = Handler.Sequence(listOfNotNull(
            ws?.let { ws.toJettyWsHandler(server) },
            sse?.let { sse.toJettySseHandler() },
            http?.let { http.toJettyHandler(stopMode is Graceful) }
        ))

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                when (stopMode) {
                    is Graceful -> server.apply { stopTimeout = stopMode.timeout.toMillis() }
                    is Immediate -> server.apply { stopTimeout = 0 }
                }
                server.start()
            }

            override fun stop(): Http4kServer = apply { server.stop() }

            override fun port(): Int = if (port > 0) port else server.uri.port
        }
    }
}
