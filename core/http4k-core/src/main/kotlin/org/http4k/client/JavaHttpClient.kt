package org.http4k.client

import org.http4k.client.PreCannedJavaHttpClients.defaultJavaHttpClient
import org.http4k.core.Body
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.net.ConnectException
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect.NEVER
import java.net.http.HttpClient.Version.HTTP_1_1
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.HttpTimeoutException
import java.nio.ByteBuffer
import java.util.Locale.getDefault

/**
 * Basic JDK-based Client.
 */
object JavaHttpClient {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        httpClient: HttpClient = defaultJavaHttpClient(),
        requestBodyMode: BodyMode = Memory,
        responseBodyMode: BodyMode = Memory,
        requestModifier: (HttpRequest.Builder) -> HttpRequest.Builder = { it },
    ) = object : HttpHandler {
        override fun invoke(request: Request): Response = try {
            val javaRequest = request.toJavaHttpRequest(requestBodyMode, requestModifier)
            when (responseBodyMode) {
                is Memory -> httpClient.send(javaRequest, BodyHandlers.ofByteArray())
                    .run { coreResponse().body(Body(ByteBuffer.wrap(body()))) }
                is Stream -> httpClient.send(javaRequest, BodyHandlers.ofInputStream())
                    .run { coreResponse().body(body()) }
            }
        } catch (e: UnknownHostException) {
            Response(UNKNOWN_HOST.toClientStatus(e))
        } catch (e: ConnectException) {
            Response(CONNECTION_REFUSED.toClientStatus(e))
        } catch (e: HttpTimeoutException) {
            Response(CLIENT_TIMEOUT.toClientStatus(e))
        }
    }
}

object PreCannedJavaHttpClients {
    /**
     * Standard non-redirecting HTTP client
     */
    fun defaultJavaHttpClient(): HttpClient = HttpClient.newBuilder()
        .version(HTTP_1_1)
        .followRedirects(NEVER)
        .build()
}

private fun Request.toJavaHttpRequest(bodyMode: BodyMode, requestModifier: (HttpRequest.Builder) -> HttpRequest.Builder) =
    HttpRequest.newBuilder()
        .uri(URI.create(uri.toString()))
        .apply {
            headers
                .filterNot { disallowedHeaders.contains(it.first.lowercase(getDefault())) }
                .fold(this) { acc, next -> acc.header(next.first, next.second) }
        }.method(method.name, body.toRequestPublisher(bodyMode))
        .let(requestModifier)
        .build()

private fun <T> HttpResponse<T>.coreResponse() =
    Response(Status(statusCode(), "")).headers(headers().map()
        .map { headerNameToValues ->
            headerNameToValues.value
                .map { headerNameToValues.key to it }
        }
        .flatten())

private fun Body.toRequestPublisher(bodyMode: BodyMode) = when (bodyMode) {
    Memory -> HttpRequest.BodyPublishers.ofByteArray(payload.array())
    Stream -> HttpRequest.BodyPublishers.ofInputStream { stream }
}

// list copied from internal JDK Utils.ALLOWED_HEADERS
private val disallowedHeaders = setOf(
    "connection", "content-length",
    "date", "expect", "from", "host", "upgrade", "via", "warning"
)
