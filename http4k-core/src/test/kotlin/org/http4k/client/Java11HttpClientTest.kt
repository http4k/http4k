package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.http4k.core.Body
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import org.http4k.server.Jetty
import org.junit.jupiter.api.Disabled
import java.net.ConnectException
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpClient.Redirect.NEVER
import java.net.http.HttpClient.Version.HTTP_1_1
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.ByteBuffer


class Java11HttpClientTest : HttpClientContract({ Jetty(it) }, Java11HttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(100))
                .build()
        ).build()
        , responseBodyMode = Stream)) {

    @Disabled("unsupported by the underlying java client")
    override fun `handles response with custom status message`() {
        super.`handles response with custom status message`()
    }
}

class Java11HttpClient(private val httpClient: HttpClient = defaultClient(),
                       private val requestBodyMode: BodyMode = Memory,
                       private val responseBodyMode: BodyMode = Memory
) : HttpHandler {
    override fun invoke(request: Request): Response = try {
        val javaRequest = request.toJavaHttpRequest(requestBodyMode)
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
    }

    companion object {
        private fun defaultClient() = HttpClient.newBuilder()
            .version(HTTP_1_1)
            .followRedirects(NEVER)
            .build()
    }
}

private fun Request.toJavaHttpRequest(bodyMode: BodyMode) =
    HttpRequest.newBuilder()
        .uri(URI.create(uri.toString()))
        .apply {
            headers.fold(this) { acc, next -> acc.header(next.first, next.second) }
        }.method(method.name, body.toRequestPublisher(bodyMode)).build()

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
