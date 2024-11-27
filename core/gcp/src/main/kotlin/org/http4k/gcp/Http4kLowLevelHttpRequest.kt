package org.http4k.gcp

import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

internal class Http4kLowLevelHttpRequest(
    private val http: HttpHandler,
    private var request: Request
) : LowLevelHttpRequest() {
    override fun addHeader(name: String, value: String?) {
        request = request.header(name, value)
    }

    override fun execute(): LowLevelHttpResponse {
        contentType?.let { request = request.header("Content-Type", it) }
        contentEncoding?.let { request = request.header("Content-Encoding", it) }
        streamingContent?.let { content ->
            val outputStream = PipedOutputStream()
            val inputStream = PipedInputStream(outputStream)

            thread {
                outputStream.use { os -> content.writeTo(os) }
            }

            request = request.body(inputStream)
        }
        return Http4kLowLevelHttpResponse(http(request))
    }
}
