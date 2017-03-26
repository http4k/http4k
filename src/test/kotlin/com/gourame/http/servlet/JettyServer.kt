package com.gourame.http.servlet

import com.gourame.http.core.HttpHandler
import com.gourame.http.servlet.asServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder

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