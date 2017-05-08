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
import org.apache.http.util.EntityUtils.toByteArray
import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.URI
import java.nio.ByteBuffer

class ApacheHttpClient(val client: CloseableHttpClient = HttpClients.createDefault()) : HttpHandler {

    override fun invoke(request: Request): Response = client.execute(request.toApacheRequest()).toUtterlyIdleResponse()

    private fun CloseableHttpResponse.toUtterlyIdleResponse(): Response =
        Response(statusLine.toTarget(), allHeaders.toTarget(), entity.toTarget())

    private fun Request.toApacheRequest(): HttpRequestBase {
        return object : HttpEntityEnclosingRequestBase() {
            init {
                val request = this@toApacheRequest
                uri = URI(request.uri.toString())
                entity = ByteArrayEntity(request.body.toString().toByteArray())
                request.headers.filter { it.first != "content-length" }.map { addHeader(it.first, it.second) }
                config = RequestConfig.custom()
                    .setRedirectsEnabled(false)
                    .setCookieSpec(IGNORE_COOKIES).build()
            }

            override fun getMethod(): String = this@toApacheRequest.method.name
        }
    }

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun HttpEntity.toTarget(): Body = ByteBuffer.wrap(toByteArray(this))

    private fun Array<Header>.toTarget(): Headers = listOf(*this.map { it.name to it.value }.toTypedArray())
}