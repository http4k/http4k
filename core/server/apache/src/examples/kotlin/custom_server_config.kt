package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.net.InetAddress
import java.time.Duration

/**
 * Support for creating a custom Apache server with address and canonical hostname.
 */
class CustomApacheServer(
    val port: Int = 8000,
    val address: InetAddress,
    val canonicalHostname: String = "localhost",
    override val stopMode: StopMode = StopMode.Graceful(Duration.ofSeconds(5))
) : ServerConfig {

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server = defaultBootstrap(port, http, canonicalHostname)
            .setLocalAddress(address)
            .create()

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            server.stopWith(stopMode)
        }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}
