package org.http4k.server

import io.helidon.nima.webserver.WebServer
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode.Immediate

class Helidon(val port: Int = 8000) : ServerConfig {
    override val stopMode = Immediate

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server = WebServer.builder()
//            .routing(NimaMain::routing)
            .start()

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            server.stop()
        }

        override fun port(): Int = if (port != 0) port else server.port()
    }
}
