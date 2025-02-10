package org.http4k.servlet.jakarta

import dev.forkhandles.mock4k.mock
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import java.util.*
import java.util.Collections.enumeration

class FakeJakartaHttpServletRequest(private val request: Request) :
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

    override fun getProtocol(): String = request.uri.scheme
}


class FakeJakartaHttpServletResponse : HttpServletResponse by mock() {
    var http4k = Response(OK)

    override fun addHeader(p0: String, p1: String?) {
        http4k = http4k.header(p0, p1!!)
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
        http4k = http4k.status(Status(sc, ""))
    }
}
