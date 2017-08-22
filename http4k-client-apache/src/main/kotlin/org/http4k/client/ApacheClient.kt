package org.http4k.client

import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.apache.http.client.config.CookieSpecs.IGNORE_COOKIES
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.http4k.core.*
import java.net.URI

class ApacheClient(private val client: CloseableHttpClient = defaultApacheHttpClient()) : HttpHandler {

    override fun invoke(request: Request): Response = client.execute(request.toApacheRequest()).toHttp4kResponse()

    private fun CloseableHttpResponse.toHttp4kResponse(): Response =
        allHeaders.toTarget().fold(Response(statusLine.toTarget()).body(entity?.toTarget() ?: Body.EMPTY)) {
            memo, (first, second) ->
            memo.header(first, second)
        }

    private fun Request.toApacheRequest(): HttpRequestBase {
        return object : HttpEntityEnclosingRequestBase() {
            init {
                val request = this@toApacheRequest
                uri = URI(request.uri.toString())
                entity = InputStreamEntity(request.body.stream)
                request.headers.filter { !it.first.equals("content-length", true) }.map { addHeader(it.first, it.second) }
            }

            override fun getMethod(): String = this@toApacheRequest.method.name
        }
    }

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun HttpEntity.toTarget(): Body = StreamBody(content)

    private fun Array<Header>.toTarget(): Headers = listOf(*this.map { it.name to it.value }.toTypedArray())

    companion object {
        private fun defaultApacheHttpClient() = HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(IGNORE_COOKIES)
            .build()).build()

    }
}
