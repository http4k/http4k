package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer

class JavaHttpClient : HttpHandler {
    override fun invoke(request: Request): Response {
        val connection = (URL(request.uri.toString()).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = request.method.name
            doOutput = true
            doInput = true
            request.headers.forEach {
                addRequestProperty(it.first, it.second)
            }
            if (request.body != Body.EMPTY) {
                request.body.stream.copyTo(outputStream)
            }
        }

        val status = Status(connection.responseCode, connection.responseMessage.orEmpty())

        val baseResponse = Response(status).body(connection.body(status))

        return connection.headerFields
            .filterKeys { it != null } // because response status line comes as a header with null key (*facepalm*)
            .map { header -> header.value.map { header.key to it } }
            .flatten()
            .fold(baseResponse, { response, nextHeader ->
                response.header(nextHeader.first, nextHeader.second)
            })
    }

    // Because HttpURLConnection closes the stream if a new request is made, we are forced to consume it straight away
    private fun HttpURLConnection.body(status: Status) =
        resolveStream(status).readBytes().let { ByteBuffer.wrap(it) }.let { Body(it) }

    private fun HttpURLConnection.resolveStream(status: Status) =
        when {
            status.serverError || status.clientError -> errorStream
            else -> inputStream
        }
}