package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.UnsupportedStopMode
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerSpec
import ratpack.server.ServerConfig.builder

class Ratpack(port: Int = 8000, stopMode: StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Immediate)

    init {
        when (stopMode) {
            is Graceful -> throw UnsupportedStopMode(stopMode)
            else -> {}
        }
    }

    private val serverConfig = builder().connectQueueSize(1000).port(port)

    override fun toServer(http: HttpHandler): Http4kServer {
        val server = RatpackServer.of { server: RatpackServerSpec ->
            server.serverConfig(serverConfig)
                .handler { RatpackHttp4kHandler(http) }
        }

        return object : Http4kServer {
            override fun start() = apply { server.start() }

            override fun stop() = apply { server.stop() }

            override fun port() = server.bindPort
        }
    }
}
