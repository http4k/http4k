package org.http4k.server

import org.eclipse.jetty.server.Server
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.time.Duration

fun JettyLoom(port: Int) = JettyLoom(port, defaultStopMode)

fun JettyLoom(port: Int, stopMode: StopMode) = Jetty(port, stopMode, Server(LoomThreadPool()))

class JettyLoomSimple(private val port: Int) : ServerConfig {
    private val server = Server(LoomThreadPool())

    override val stopMode = defaultStopMode

    override fun toServer(http: HttpHandler) = server.run {
        addConnector(http(port)(this))
        insertHandler(http.toJettyHandler(true))
        object : Http4kServer {
            override fun start(): Http4kServer = apply { server.start() }
            override fun stop(): Http4kServer = apply { server.stop() }
            override fun port(): Int = if (port > 0) port else server.uri.port
        }
    }
}

internal val defaultStopMode = ServerConfig.StopMode.Graceful(Duration.ofSeconds(5))
