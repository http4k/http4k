package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet

/**
 * Exposed to allow for customisation of the Jetty server instance
 */
class Http4kJettyServer(port: Int = 8000, app: HttpHandler) : Server(port) {
    init {
        handler = ServletContextHandler(SESSIONS).apply {
            addServlet(ServletHolder(app.asServlet()), "/*")
        }
    }
}

data class Jetty(val port: Int = 8000) : ServerConfig {
    override fun toServer(handler: HttpHandler): Http4kServer {
        return object : Http4kServer {
            private val server = Http4kJettyServer(port, handler)

            override fun start(): Http4kServer {
                server.start()
                return this
            }

            override fun block(): Http4kServer {
                Thread.currentThread().join()
                return this
            }

            override fun stop() = server.stop()
        }
    }
}
