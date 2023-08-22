package org.http4k.servlet

import dev.forkhandles.mock4k.mock
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import java.util.Collections.enumeration
import java.util.Enumeration
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class FakeHttpServletRequest(private val request: Request) :
    HttpServletRequest by mock<HttpServletRequest>() {
    override fun getMethod() = request.method.name
    override fun getRequestURI() = request.uri.toString()
    override fun getQueryString() = request.uri.query
    override fun getHeaderNames(): Enumeration<String> = enumeration(request.headers.map { it.first })
    override fun getHeader(name: String) = request.header(name)
    override fun getHeaders(name: String): Enumeration<String> = enumeration(request.headerValues(name))
    override fun getInputStream(): ServletInputStream = object : ServletInputStream() {
        private val stream = request.body.stream

        override fun read() = stream.read()

        override fun isFinished() = stream.available() > 0

        override fun isReady() = true

        override fun setReadListener(readListener: ReadListener?) = TODO("Not yet implemented")
    }

    override fun getRemoteAddr() = request.uri.host
    override fun getRemotePort() = request.uri.port ?: 0
    override fun getScheme() = request.uri.scheme
}

class FakeHttpServletResponse : HttpServletResponse by mock<HttpServletResponse>() as HttpServletResponse {
    var http4k = Response(OK)

    override fun setStatus(sc: Int, sm: String) {
        http4k = http4k.status(Status(sc, sm))
    }

    override fun getOutputStream() = object : ServletOutputStream() {
        override fun write(b: Int) {
            http4k = http4k.body(String(http4k.bodyString().toByteArray() + b.toByte()))
        }

        override fun isReady() = true

        override fun setWriteListener(writeListener: WriteListener?) {
            TODO("Not yet implemented")
        }
    }

    override fun setStatus(sc: Int) {
        setStatus(sc, "")
    }
}
