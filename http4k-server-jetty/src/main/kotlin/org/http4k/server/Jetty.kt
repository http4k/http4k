package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet

data class Jetty(val port: Int = 8000) : ServerConfig {
    override fun toServer(handler: HttpHandler): Http4kServer {
        return object : Http4kServer {
            private val server = Server(port).apply {
                setHandler(ServletContextHandler(ServletContextHandler.SESSIONS).apply {
                    addServlet(ServletHolder(handler.asServlet()), "/*")
                })
            }

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
