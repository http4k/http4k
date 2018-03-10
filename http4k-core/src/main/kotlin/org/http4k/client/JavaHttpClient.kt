package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.HttpURLConnection
import java.net.URL

class JavaHttpClient : HttpHandler {
    override fun invoke(request: Request): Response {
        val con = (URL(request.uri.toString()).openConnection() as HttpURLConnection).apply {
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

        val status = Status(con.responseCode, con.responseMessage.orEmpty())

        val baseResponse = Response(status).body(if (status.successful) {
            con.inputStream
        } else {
            con.errorStream
        })

        return con.headerFields
            .filterKeys { it != null } // because response status line comes as a header with null key (*facepalm*)
            .map { header -> header.value.map { header.key to it } }
            .flatten()
            .fold(baseResponse, { response, nextHeader ->
                response.header(nextHeader.first, nextHeader.second)
            })
    }
}