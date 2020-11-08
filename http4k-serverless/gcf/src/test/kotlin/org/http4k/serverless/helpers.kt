package org.http4k.serverless

import com.google.cloud.functions.HttpRequest
import com.google.cloud.functions.HttpResponse
import org.http4k.core.Request
import java.io.ByteArrayOutputStream
import java.util.Optional

class FakeGCFRequest(private val http4k: Request) : HttpRequest {
    override fun getReader() = TODO()
    override fun getMethod() = http4k.method.name
    override fun getHeaders(): Map<String, List<String>> = http4k.headers
        .groupBy { it.first }
        .mapValues { it.value.map { it.second ?: "" } }
        .toMutableMap()

    override fun getUri() = http4k.uri.toString()
    override fun getCharacterEncoding() = TODO()
    override fun getQuery() = TODO()
    override fun getContentLength() = http4k.body.length ?: -1
    override fun getContentType() = TODO()
    override fun getPath() = http4k.uri.path
    override fun getParts() = TODO()
    override fun getQueryParameters() = TODO()
    override fun getInputStream() = http4k.body.stream
}

class FakeGCFResponse : HttpResponse {
    var status: Int? = null
    private var _contentType: Optional<String> = Optional.empty()
    private val headers = mutableMapOf<String, MutableList<String>>()
    private val outStream = ByteArrayOutputStream()

    override fun getOutputStream() = outStream
    override fun getHeaders() = headers
    override fun getContentType() = _contentType
    override fun getWriter() = TODO()
    override fun setStatusCode(code: Int) = TODO()

    val body: String get() = String(outStream.toByteArray())

    override fun setContentType(contentType: String) {
        _contentType = Optional.of(contentType)
    }

    override fun appendHeader(header: String, value: String) {
        headers.getOrPut(header, { mutableListOf() }).add(value)
    }

    override fun setStatusCode(code: Int, message: String?) {
        status = code
    }
}
