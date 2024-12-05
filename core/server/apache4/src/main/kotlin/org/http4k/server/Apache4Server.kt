package org.http4k.server

import org.apache.http.config.SocketConfig
import org.apache.http.impl.bootstrap.ServerBootstrap
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.util.concurrent.TimeUnit.MILLISECONDS

class Apache4Server(val port: Int = 8000, override val stopMode: StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Immediate)

    init {
        if (stopMode != StopMode.Immediate) {
            throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server = createBootstrap(http, port).create()

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            server.shutdown(0, MILLISECONDS)
        }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}


fun createBootstrap(http: HttpHandler, port: Int): ServerBootstrap =
    ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setSocketConfig(
            SocketConfig.custom()
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)
                .setSoReuseAddress(true)
                .setBacklogSize(1000)
                .build()
        )
        .registerHandler("*", Http4kApache4RequestHandler(http))
