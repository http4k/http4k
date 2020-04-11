package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.Body
import org.http4k.core.BodyMode
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


class Java11HttpClientTest : HttpClientContract({ Jetty(it) }, Java11HttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultSocketConfig(
            SocketConfig.custom()
                .setSoTimeout(100)
                .build()
        ).build()
        , responseBodyMode = BodyMode.Stream)) {

    @Disabled("unsupported by the underlying java client")
    override fun `handles response with custom status message`() {
        super.`handles response with custom status message`()
    }
}

class Java11HttpClient(private val httpClient: HttpClient = defaultClient(),
                       private val requestBodyMode: BodyMode = BodyMode.Memory,
                       private val responseBodyMode: BodyMode = BodyMode.Memory
                       ) : HttpHandler {
    override fun invoke(request: Request): Response = try {
        httpClient
            .send(request.toJavaHttpRequest(requestBodyMode), responseBodyMode.toBodyHandler())
            .toResponse()
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

private fun Request.toJavaHttpRequest(bodyMode: BodyMode): HttpRequest =
    HttpRequest.newBuilder()
        .uri(URI.create(uri.toString()))
        .apply {
            headers.fold(this) { acc, next -> acc.header(next.first, next.second) }
        }.method(method.name, body.toRequestPublisher(bodyMode)).build()

private fun HttpResponse<String>.toResponse(): Response {
    val headers = headers().map()
        .map { headerNameToValues -> headerNameToValues.value.map { headerNameToValues.key to it } }
        .flatten()
    return Response(Status(statusCode(), "")).headers(headers).body(body())
}

private fun Body.toRequestPublisher(bodyMode: BodyMode) = when (bodyMode) {
    BodyMode.Memory -> HttpRequest.BodyPublishers.ofByteArray(payload.array())
    BodyMode.Stream -> HttpRequest.BodyPublishers.ofInputStream { stream }
}

private fun BodyMode.toBodyHandler() = when(this){
    BodyMode.Memory -> BodyHandlers.ofByteArray()
    BodyMode.Stream -> BodyHandlers.ofInputStream()
}
