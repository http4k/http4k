package org.http4k.server

import io.helidon.webserver.Routing
import io.helidon.webserver.websocket.WsRoute
import io.helidon.webserver.websocket.WsRouting
import org.http4k.websocket.WsHandler


class Http4kWsRouting(private val ws: WsHandler) : Routing {
    override fun routingType() = WsRouting::class.java

    class Builder(ws: WsHandler) : io.helidon.common.Builder<Builder, WsRouting> {
        override fun build(): WsRouting {
            val builder: WsRouting.Builder = WsRouting.builder()
            val method = builder::class.java.getDeclaredMethod("route", WsRoute::class.java)
            method.isAccessible = true
            return builder.build()
        }
    }
}
