package org.http4k.client

import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.apache.http.client.config.CookieSpecs.IGNORE_COOKIES
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.SocketTimeoutException
import java.net.URI

class ApacheClient(private val client: CloseableHttpClient = defaultApacheHttpClient(), private val bodyMode: ResponseBodyMode = ResponseBodyMode.Memory) : HttpHandler {

    override fun invoke(request: Request): Response = try {
        client.execute(request.toApacheRequest()).toHttp4kResponse()
    } catch (e: SocketTimeoutException) {
        Response(Status.CLIENT_TIMEOUT)
    }

    private fun CloseableHttpResponse.toHttp4kResponse(): Response =
        allHeaders.toTarget().fold(Response(statusLine.toTarget()).body(entity?.toTarget() ?: Body.EMPTY)) { memo, (first, second) ->
            memo.header(first, second)
        }

    private fun Request.toApacheRequest(): HttpRequestBase {
        return object : HttpEntityEnclosingRequestBase() {
            init {
                val request = this@toApacheRequest
                uri = URI(request.uri.toString())
                entity = ByteArrayEntity(request.body.payload.array())
                request.headers.filter { !it.first.equals("content-length", true) }.map { addHeader(it.first, it.second) }
            }

            override fun getMethod(): String = this@toApacheRequest.method.name
        }
    }

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun HttpEntity.toTarget(): Body = bodyMode(content)

    private fun Array<Header>.toTarget(): Headers = listOf(*this.map { it.name to it.value }.toTypedArray())

    companion object {
        private fun defaultApacheHttpClient() = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(IGNORE_COOKIES)
            .build()).build()

    }
}
