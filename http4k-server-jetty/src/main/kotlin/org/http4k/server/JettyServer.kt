package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.http4k.core.HttpHandler
import org.http4k.servlet.asServlet

fun HttpHandler.asJettyServer(port: Int = 8000) = JettyServer(this, port)

fun HttpHandler.startJettyServer(port: Int = 8000, block: Boolean = true): JettyServer {
    val server = asJettyServer(port).start()
    return if (block) server.block() else server
}

class JettyServer(application: HttpHandler, port: Int) {
    private val server = Server(port).apply {
        handler = ServletContextHandler(ServletContextHandler.SESSIONS).apply {
            addServlet(ServletHolder(application.asServlet()), "/*")
        }
    }

    fun start(): JettyServer {
        server.start()
        return this
    }

    fun block(): JettyServer {
        Thread.currentThread().join()
        return this
    }

    fun stop() = server.stop()
}