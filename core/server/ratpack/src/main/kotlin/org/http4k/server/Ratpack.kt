package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.UnsupportedStopMode
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerSpec
import ratpack.server.ServerConfig.builder

/**
 * Stock version of an Ratpack Server. Not that if you want to configure your own server instance you
 * can duplicate this code and modify it as required. We are purposefully trying to limit options
 * here to keep the API simple for the 99% of use-cases.
 */
class Ratpack(port: Int = 8000, stopMode: StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Immediate)

    init {
        if (stopMode is Graceful) throw UnsupportedStopMode(stopMode)
    }

    private val serverConfig = builder().connectQueueSize(1000).port(port)

    override fun toServer(http: HttpHandler) = object : Http4kServer {
        val server = RatpackServer.of { server: RatpackServerSpec ->
            server.serverConfig(serverConfig)
                .handler { RatpackHttp4kHandler(http) }
        }

        override fun start() = apply { server.start() }

        override fun stop() = apply { server.stop() }

        override fun port() = server.bindPort
    }
}
