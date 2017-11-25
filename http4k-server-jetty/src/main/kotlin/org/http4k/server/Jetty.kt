package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet


class Jetty(private val server: Server) : ServerConfig {
    constructor(port: Int = 8000) : this(Server(port))

    override fun toServer(handler: HttpHandler): Http4kServer {
        server.insertHandler(ServletContextHandler(SESSIONS).apply {
            addServlet(ServletHolder(handler.asServlet()), "/*")
        })

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop() = server.stop()
        }
    }
}

class WsJetty(private val server: Server) {
    constructor(port: Int = 8000) : this(Server(port))

    fun toServer(handler: HttpHandler, a: WebsocketRouter): Http4kServer {

        server.insertHandler(ServletContextHandler(SESSIONS).apply {
            addServlet(ServletHolder(handler.asServlet()), "/*")
        })
        server.insertHandler(object : WebSocketHandler() {
            override fun configure(factory: WebSocketServletFactory) {
                factory.policy.idleTimeout = 10000
                factory.setCreator { req, _ ->
                    println("hello")
                    a.match(req.asHttp4kRequest())?.let { Http4kWebsocketEndpoint(it) }
                }
            }
        })

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop() = server.stop()
        }
    }
}
