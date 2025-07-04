package org.http4k.server

import io.helidon.webserver.WebServer
import io.helidon.webserver.http.HttpRouting
import io.helidon.webserver.websocket.WsRouting
import org.http4k.bridge.HelidonToHttp4kHandler
import org.http4k.bridge.HelidonToHttp4kWebSocketListener
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler

class Helidon(val port: Int = 8000, override val stopMode: StopMode) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?) =
        object : Http4kServer {
            private val server = WebServer.builder()
                .apply { if (stopMode is Graceful) shutdownGracePeriod(stopMode.timeout) }
                .addRouting(HttpRouting.builder().any(HelidonToHttp4kHandler(http, sse)))
                .apply { ws?.let { addRouting(WsRouting.builder().endpoint("*") { HelidonToHttp4kWebSocketListener(it) }) } }
                .port(port)
                .build()

            override fun start() = apply { server.start() }

            override fun stop() = apply { server.stop() }

            override fun port(): Int = if (port != 0) port else server.port()
        }
}
