package org.http4k.client

import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.config.CookieSpecs.IGNORE_COOKIES
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.concurrent.FutureCallback
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import java.net.SocketTimeoutException
import java.net.URI

object ApacheAsyncClient {
    operator fun invoke(
        client: CloseableHttpAsyncClient = defaultApacheAsyncHttpClient(),
        responseBodyMode: BodyMode = Memory,
        requestBodyMode: BodyMode = Memory
    ): AsyncHttpClient {
        if (!client.isRunning) client.start()

        return object : AsyncHttpClient {
            override fun close() = client.close()

            override fun invoke(request: Request, fn: (Response) -> Unit) {
                client.execute(request.toApacheRequest(), object : FutureCallback<HttpResponse> {
                    override fun cancelled() {}

                    override fun completed(result: HttpResponse) = fn(result.toHttp4kResponse())

                    override fun failed(e: Exception) = fn(Response(when (e) {
                        is ConnectTimeoutException -> CLIENT_TIMEOUT
                        is SocketTimeoutException -> CLIENT_TIMEOUT
                        else -> SERVICE_UNAVAILABLE
                    }.toClientStatus(e)))
                })
            }

            private fun HttpResponse.toHttp4kResponse(): Response =
                Response(statusLine.toTarget()).headers(allHeaders.toTarget()).run {
                    entity?.let { body(responseBodyMode(it.content)) } ?: this
                }

            private fun Request.toApacheRequest(): HttpRequestBase = object : HttpEntityEnclosingRequestBase() {
                init {
                    val request = this@toApacheRequest
                    uri = URI(request.uri.toString())
                    entity = when (requestBodyMode) {
                        Stream -> InputStreamEntity(request.body.stream, request.header("content-length")?.toLong()
                            ?: -1)
                        Memory -> ByteArrayEntity(request.body.payload.array())
                    }
                    request.headers.filter { !it.first.equals("content-length", true) }.map { addHeader(it.first, it.second) }
                }

                override fun getMethod(): String = this@toApacheRequest.method.name
            }

            private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

            private fun Array<Header>.toTarget(): Headers = listOf(*map { it.name to it.value }.toTypedArray())

        }
    }

    private fun defaultApacheAsyncHttpClient() = HttpAsyncClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(IGNORE_COOKIES)
            .build()).build().apply { start() }
}
