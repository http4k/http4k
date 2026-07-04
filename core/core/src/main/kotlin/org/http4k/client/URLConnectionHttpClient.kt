package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.time.Duration
import java.time.Duration.ZERO

/**
 * A raw HTTP client which directly uses a URLConnection
 */
object URLConnectionHttpClient {
    @JvmStatic
    @JvmName("create")
    operator fun invoke(): HttpHandler = invoke(ZERO)

    @JvmStatic
    @JvmName("createWithTimeouts")
    @JvmOverloads
    operator fun invoke(
        readTimeout: Duration = ZERO,
        connectionTimeout: Duration = ZERO,
        bodyMode: BodyMode = Memory
    ): HttpHandler = { request: Request ->
        try {
            val connection = (URI(request.uri.toString()).toURL().openConnection() as HttpURLConnection).apply {
                this.readTimeout = readTimeout.toMillis().toInt()
                this.connectTimeout = connectionTimeout.toMillis().toInt()
                instanceFollowRedirects = false
                requestMethod = request.method.name
                doOutput = true
                doInput = true
                request.headers.forEach {
                    addRequestProperty(it.first, it.second)
                }
                request.header("content-length")?.toLongOrNull()?.let { setFixedLengthStreamingMode(it) }
                writeBody(request.body)
            }

            val status = Status(connection.responseCode, connection.responseMessage.orEmpty())
            val baseResponse = Response(status).body(bodyMode(connection.resolveStream(status)))
            connection.headerFields
                .filterKeys { it != null } // because response status line comes as a header with null key (*facepalm*)
                .map { header -> header.value.map { header.key to it } }
                .flatten()
                .fold(baseResponse) { acc, next -> acc.header(next.first, next.second) }
        } catch (e: UnknownHostException) {
            Response(UNKNOWN_HOST.toClientStatus(e))
        } catch (e: ConnectException) {
            Response(CONNECTION_REFUSED.toClientStatus(e))
        } catch (e: SocketTimeoutException) {
            Response(CLIENT_TIMEOUT.toClientStatus(e))
        } catch (e: SocketException) {
            Response(SERVICE_UNAVAILABLE.toClientStatus(e))
        } catch (e: IOException) {
            Response(SERVICE_UNAVAILABLE.toClientStatus(e))
        }
    }

    private fun HttpURLConnection.writeBody(body: Body) {
        if (body != Body.EMPTY) {
            val content = if (body.stream.available() == 0) body.payload.array().inputStream() else body.stream
            content.copyTo(outputStream)
        }
    }

    private fun HttpURLConnection.resolveStream(status: Status) =
        when {
            status.serverError || status.clientError -> errorStream
            else -> inputStream
        } ?: EMPTY_STREAM

    private val EMPTY_STREAM = ByteArrayInputStream(ByteArray(0))
}

@Deprecated("Renamed to URLConnectionHttpClient", ReplaceWith("URLConnectionHttpClient"))
typealias Java8HttpClient = URLConnectionHttpClient
