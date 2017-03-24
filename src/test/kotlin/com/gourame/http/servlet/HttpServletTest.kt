package com.gourame.http.servlet

import com.gourame.http.apache.ApacheHttpClient
import com.gourame.http.core.Entity
import com.gourame.http.core.Headers
import com.gourame.http.core.HttpHandler
import com.gourame.http.core.Method
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status
import com.gourame.http.core.Uri
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
        val server = BetterServer({ request -> Response(Status.OK, mapOf(), Entity("Hello World")) }, 8000).start()
        val client = ApacheHttpClient()
        assertThat(client(Request(Method.GET, Uri("http://localhost:8000/"))).entity, equalTo(Entity("Hello World")))
        server.stop()
    }
}

class BetterHttpServlet(private val handler: HttpHandler) : HttpServlet() {
    override fun service(req: HttpServletRequest, resp: HttpServletResponse) =
        transfer(handler(req.asServletRequest()), resp)

    private fun transfer(source: Response, destination: HttpServletResponse): Unit {
        val status = source.status
        destination.setStatus(status.code, status.description)
        for ((key, value) in source.headers) {
            destination.addHeader(key, value)
        }
        destination.outputStream.write(source.entity.value)
    }

    private fun HttpServletRequest.asServletRequest(): Request =
        Request(Method.valueOf(method),
            Uri(requestURI + queryString.toQueryString().orEmpty()),
            headerParameters(),
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