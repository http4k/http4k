package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet

class Jetty(private val server: Server) : ServerConfig {
    constructor(port: Int = 8000) : this(Server(port))

    override fun toServer(handler: HttpHandler): Http4kServer {
        server.insertHandler(ServletContextHandler(SESSIONS).apply {
            addServlet(ServletHolder(handler.asServlet()), "/*")
        })

        return object : Http4kServer {
            override fun start(): Http4kServer {
                server.start()
                return this
            }

            override fun stop() = server.stop()
        }
    }
}
