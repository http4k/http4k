package com.gourame.http.apache

import com.gourame.http.core.Entity
import com.gourame.http.core.Headers
import com.gourame.http.core.HttpHandler
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils.toByteArray
import java.net.URI

class ApacheHttpClient(val client: CloseableHttpClient = HttpClients.createDefault()) : HttpHandler {

    override fun invoke(request: Request): Response = client.execute(request.toApacheRequest()).toUtterlyIdleResponse()

    private fun CloseableHttpResponse.toUtterlyIdleResponse(): Response =
        Response(statusLine.toTarget(), allHeaders.toTarget(), entity.toTarget())

    private fun Request.toApacheRequest(): HttpRequestBase {
        return object : HttpEntityEnclosingRequestBase() {
            init {
                val request = this@toApacheRequest
                uri = URI(request.uri.toString())
                entity = ByteArrayEntity(request.entity.toString().toByteArray())
                request.headers.minus("content-length").map { addHeader(it.key, it.value) }
            }

            override fun getMethod(): String = this@toApacheRequest.method.name
        }
    }

    private fun CloseableHttpResponse.apacheStatus(): Status = statusLine.toTarget()

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun HttpEntity.toTarget(): Entity = Entity(toByteArray(this))

    private fun Array<Header>.toTarget(): Headers = mapOf(*this.map { it.name to it.value }.toTypedArray())
}