package org.http4k.client

import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.HttpHostConnectException
import org.apache.hc.client5.http.classic.methods.*
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.io.entity.ByteArrayEntity
import org.apache.hc.core5.http.io.entity.InputStreamEntity
import org.http4k.core.*
import org.http4k.core.BodyMode.Memory
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method.*
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

object ApacheClient5 {
    operator fun invoke(
        client: CloseableHttpClient = defaultApacheHttpClient(),
        responseBodyMode: BodyMode = Memory,
        requestBodyMode: BodyMode = Memory
    ): HttpHandler = { request ->
        try {
            client.execute(request.toApacheRequest(requestBodyMode)).toHttp4kResponse(responseBodyMode)
        } catch (e: ConnectTimeoutException) {
            Response(CLIENT_TIMEOUT.toClientStatus(e))
        } catch (e: SocketTimeoutException) {
            Response(CLIENT_TIMEOUT.toClientStatus(e))
        } catch (e: HttpHostConnectException) {
            Response(CONNECTION_REFUSED.toClientStatus(e))
        } catch (e: UnknownHostException) {
            Response(UNKNOWN_HOST.toClientStatus(e))
        }
    }

    private fun Request.toApacheRequest(requestBodyMode: BodyMode): HttpUriRequestBase {
        val request = this@toApacheRequest
        val uri = URI(request.uri.toString())

        val apacheRequest = when (method) {
            HEAD -> HttpHead(uri)
            GET -> HttpGet(uri)
            OPTIONS -> HttpOptions(uri)
            TRACE -> HttpTrace(uri)
            DELETE -> HttpDelete(uri)
            else -> ApacheRequest(requestBodyMode, request)
        }
        request.headers.filter { !it.first.equals("content-length", true) }.map { apacheRequest.addHeader(it.first, it.second) }
        return apacheRequest
    }

    private fun HttpResponse.toTargetStatus() = Status(code, reasonPhrase)

    private fun Array<Header>.toTarget(): Headers = listOf(*map { it.name to it.value }.toTypedArray())

    private fun CloseableHttpResponse.toHttp4kResponse(responseBodyMode: BodyMode) = with(Response(toTargetStatus()).headers(headers.toTarget())) {
        entity?.let { body(responseBodyMode(it.content)) } ?: this
    }

    private fun defaultApacheHttpClient() = HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(StandardCookieSpec.IGNORE)
            .build()).build()

}

private class ApacheRequest(requestBodyMode: BodyMode, private val request: Request) : HttpUriRequestBase(request.method.toString(), URI(request.uri.toString())) {
    init {
        entity = when (requestBodyMode) {
            Stream -> InputStreamEntity(request.body.stream, request.header("content-length")?.toLong() ?: -1, null)
            Memory -> ByteArrayEntity(request.body.payload.array(), null)
        }
    }

    override fun getMethod() = request.method.name
}
