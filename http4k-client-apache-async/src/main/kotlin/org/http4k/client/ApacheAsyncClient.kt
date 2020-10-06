package org.http4k.client

import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpResponse
import org.http4k.core.*
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import java.io.ByteArrayInputStream
import java.net.SocketTimeoutException

object ApacheAsyncClient {
    operator fun invoke(
        client: CloseableHttpAsyncClient = defaultApacheAsyncHttpClient()
    ): AsyncHttpClient {
        return object : AsyncHttpClient {
            override fun close() = client.close()

            override fun invoke(request: Request, fn: (Response) -> Unit) {
                client.execute(request.toApacheRequest(), object : FutureCallback<SimpleHttpResponse> {
                    override fun cancelled() {}

                    override fun completed(result: SimpleHttpResponse) = fn(result.toHttp4kResponse())

                    override fun failed(e: Exception) = fn(Response(when (e) {
                        is ConnectTimeoutException -> CLIENT_TIMEOUT
                        is SocketTimeoutException -> CLIENT_TIMEOUT
                        else -> SERVICE_UNAVAILABLE
                    }.toClientStatus(e)))
                })
            }

            private fun SimpleHttpResponse.toHttp4kResponse(): Response =
                Response(toTargetStatus()).headers(headers.toTarget()).run {
                    this@toHttp4kResponse.body?.let { body(ByteArrayInputStream(it.bodyBytes)) } ?: this
                }

            private fun Request.toApacheRequest(): SimpleHttpRequest = this@toApacheRequest.let { http4kRequest ->
                val apacheRequest = SimpleHttpRequest(http4kRequest.method.name, http4kRequest.uri.toString())
                apacheRequest.setBody(http4kRequest.body.payloadAsByteArray(), ContentType.getByMimeType(http4kRequest.header("content-type")))
                headers.filter { !it.first.equals("content-length", true) }.map { apacheRequest.addHeader(it.first, it.second) }
                apacheRequest
            }

            private fun HttpResponse.toTargetStatus() = Status(code, reasonPhrase)

            private fun Array<Header>.toTarget(): Headers = listOf(*map { it.name to it.value }.toTypedArray())

            private fun Body.payloadAsByteArray(): ByteArray = if (payload.hasArray()) {
                payload.array()
            } else {
                val bytesArray = ByteArray(payload.remaining())
                payload.get(bytesArray, 0, bytesArray.size)
                bytesArray
            }
        }
    }

    private fun defaultApacheAsyncHttpClient() = HttpAsyncClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(StandardCookieSpec.IGNORE)
            .build()).build().apply { start() }
}
