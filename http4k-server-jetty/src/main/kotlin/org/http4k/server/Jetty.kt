package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet
import org.http4k.websocket.WsHandler

class Jetty(private val server: Server) : WsServerConfig {
    constructor(port: Int = 8000) : this(Server(port))

    override fun toServer(httpHandler: HttpHandler?, wsHandler: WsHandler?): Http4kServer {
        httpHandler?.let { server.insertHandler(httpHandler.toJettyHandler()) }
        wsHandler?.let { server.insertHandler(it.toJettyHandler()) }

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop() = server.stop()
        }
    }
}

private fun WsHandler.toJettyHandler() = object : WebSocketHandler() {
    override fun configure(factory: WebSocketServletFactory) {
        factory.setCreator { req, _ ->
            this@toJettyHandler(req.asHttp4kRequest())?.let(::Http4kWebSocketListener)
        }
    }
}

private fun HttpHandler.toJettyHandler() = ServletContextHandler(SESSIONS).apply {
    addServlet(ServletHolder(this@toJettyHandler.asServlet()), "/*")
}