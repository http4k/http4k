package com.gourame.http.servlet

import com.gourame.http.core.Entity
import com.gourame.http.core.Headers
import com.gourame.http.core.HttpHandler
import com.gourame.http.core.Method
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status
import com.gourame.http.core.Uri
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.Test
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class HttpServletTest {


    @Test
    fun can_use_as_servlet() {
        BetterServer({ request -> Response(Status.OK, mapOf(), Entity("Hello World")) }, 8000).start().block()
    }
}

class BetterHttpServlet(private val handler: HttpHandler) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) =
        transfer(handler(req.toHttpServletRequest()), resp)

    private fun transfer(source: Response, destination: HttpServletResponse): Unit {
        val status = source.status
        destination.setStatus(status.code, status.description)
        for ((key, value) in source.headers) {
            destination.addHeader(key, value)
        }
        source.headers["Content-Length"]?.let {
            destination.setContentLength(Integer.parseInt(it))
        }
        destination.outputStream.write(source.entity.toString().toByteArray())
    }

    private fun HttpServletRequest.toHttpServletRequest(): Request =
        Request(Method.valueOf(method),
            Uri(requestURI + queryString.toQueryString().orEmpty()),
            this.headerParameters(),
            Entity(inputStream.readBytes())
        )

    private fun HttpServletRequest.headerParameters(): Headers = this.headerNames.asSequence().map { it to this.getHeader(it) }.toMap()

    private fun String?.toQueryString(): String = if (this != null && this.isNotEmpty()) "?" + this else ""
}

class BetterServer(application: HttpHandler, port: Int) {
    private val server = Server(port).apply {
        handler = ServletContextHandler(ServletContextHandler.SESSIONS).apply {
            addServlet(ServletHolder(BetterHttpServlet(application)), "/*")
        }
    }

    fun start(): BetterServer {
        server.start()
        return this
    }

    fun block(): BetterServer {
        Thread.currentThread().join()
        return this
    }

    fun stop() = server.stop()
}