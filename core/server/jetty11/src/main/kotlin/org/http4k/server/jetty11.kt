package org.http4k.server

import org.eclipse.jetty.server.Server
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

/**
 * Stock version of an Jetty11 Server. Not that if you want to configure your own server instance you
 * can duplicate this code and modify it as required. We are purposefully trying to limit options
 * here to keep the API simple for the 99% of use-cases.
 */
class Jetty11(private val port: Int, override val stopMode: StopMode, private val server: Server) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, defaultStopMode)
    constructor(port: Int = 8000, stopMode: StopMode) : this(port, stopMode, http(port))
    constructor(port: Int = 8000, server: Server) : this(port, defaultStopMode, server)
    constructor(port: Int, vararg inConnectors: ConnectorBuilder) : this(port, defaultStopMode, *inConnectors)

    constructor(port: Int, stopMode: StopMode, vararg inConnectors: ConnectorBuilder) : this(
        port,
        stopMode,
        Server().apply { inConnectors.forEach { addConnector(it(this)) } })

    init {
        when (stopMode) {
            is Graceful -> server.apply { stopTimeout = stopMode.timeout.toMillis() }
            is Immediate -> server.apply { stopAtShutdown = true }
        }
    }

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        http?.let { server.insertHandler(http.toJetty11Handler(stopMode is Graceful)) }
        sse?.let { server.insertHandler(sse.toJettySseHandler()) }
        ws?.let { server.insertHandler(ws.toJettyWsHandler()) }

        return object : Http4kServer {
            override fun start(): Http4kServer = apply { server.start() }

            override fun stop(): Http4kServer = apply { server.stop() }

            override fun port(): Int = if (port > 0) port else server.uri.port
        }
    }
}
