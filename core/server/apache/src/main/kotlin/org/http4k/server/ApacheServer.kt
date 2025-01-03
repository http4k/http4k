package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.time.Duration

/**
 * Stock version of an Apache Server. Not that if you want to configure your own server instance you
 * can duplicate this code and modify it as required. We are purposefully trying to limit options
 * here to keep the API simple for the 99% of use-cases.
 */
class ApacheServer(private val port: Int, override val stopMode: StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Graceful(Duration.ofSeconds(5)))

    private val canonicalHostname = "localhost"

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server = defaultBootstrap(port, http, canonicalHostname).create()

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            server.stopWith(stopMode)
        }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}

