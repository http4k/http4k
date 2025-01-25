package org.http4k.server

import org.apache.http.impl.bootstrap.HttpServer
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.net.InetAddress
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Custom server config with address and stop mode.
 */
class CustomApache4Server(
    val port: Int = 8000,
    override val stopMode: StopMode,
    val address: InetAddress
) : ServerConfig {

    init {
        if (stopMode != StopMode.Immediate) {
            throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server: HttpServer = createBootstrap(http, port)
            .setLocalAddress(address).create()

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            server.shutdown(0, MILLISECONDS)
        }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}
