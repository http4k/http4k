package org.http4k.server

import io.helidon.webserver.WebServer
import io.helidon.webserver.http.HttpRouting
import io.helidon.webserver.websocket.WsRouting
import org.http4k.bridge.HelidonToHttp4kHandler
import org.http4k.bridge.HelidonToHttp4kWebSocketListener
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.util.function.Supplier

class Helidon(val port: Int = 8000, override val stopMode: StopMode) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    init {
        if (stopMode != Immediate) throw ServerConfig.UnsupportedStopMode(stopMode)
    }

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?) =
        object : Http4kServer {
            private val server = WebServer.builder()
                .addRouting(HttpRouting.builder().any(HelidonToHttp4kHandler(http, sse)))
                .apply { ws?.let { addRouting(WsRouting.builder().endpoint("*") { HelidonToHttp4kWebSocketListener(it) }) } }
                .port(port)
                .build()

            override fun start() = apply { server.start() }

            override fun stop() = apply { server.stop() }

            override fun port(): Int = if (port != 0) port else server.port()
        }
}
