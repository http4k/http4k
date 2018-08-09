package org.http4k.client

import org.apache.http.Header
import org.apache.http.StatusLine
import org.apache.http.client.config.CookieSpecs.IGNORE_COOKIES
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpOptions
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpTrace
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode
import org.http4k.core.BodyMode.Memory
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.TRACE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

class ApacheClient(
    private val client: CloseableHttpClient = defaultApacheHttpClient(),
    private val responseBodyMode: BodyMode = Memory,
    private val requestBodyMode: BodyMode = Memory
) : HttpHandler {

    override fun invoke(request: Request): Response = try {
        client.execute(request.toApacheRequest()).toHttp4kResponse()
    } catch (e: ConnectTimeoutException) {
        Response(CLIENT_TIMEOUT.description("Client Error: caused by ${e.localizedMessage}"))
    } catch (e: SocketTimeoutException) {
        Response(CLIENT_TIMEOUT.description("Client Error: caused by ${e.localizedMessage}"))
    } catch (e: HttpHostConnectException) {
        Response(CONNECTION_REFUSED.description("Client Error: caused by ${e.localizedMessage}"))
    } catch (e: UnknownHostException) {
        Response(UNKNOWN_HOST.description("Client Error: caused by ${e.localizedMessage}"))
    }

    private fun CloseableHttpResponse.toHttp4kResponse() = with(Response(statusLine.toTarget()).headers(allHeaders.toTarget())) {
        entity?.let { body(responseBodyMode(it.content)) } ?: this
    }

    private fun Request.toApacheRequest(): HttpRequestBase {
        val request = this@toApacheRequest
        val uri = URI(request.uri.toString())

        val apacheRequest = when (method) {
            HEAD -> HttpHead(uri)
            GET -> HttpGet(uri)
            OPTIONS -> HttpOptions(uri)
            TRACE -> HttpTrace(uri)
            DELETE -> HttpDelete(uri)
            else ->
                object : HttpEntityEnclosingRequestBase() {
                    init {
                        this.uri = uri
                        entity = when (requestBodyMode) {
                            Stream -> InputStreamEntity(request.body.stream, request.header("content-length")?.toLong() ?: -1)
                            Memory -> ByteArrayEntity(request.body.payload.array())
                        }
                    }

                    override fun getMethod() = request.method.name
                }
        }
        request.headers.filter { !it.first.equals("content-length", true) }.map { apacheRequest.addHeader(it.first, it.second) }
        return apacheRequest
    }

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun Array<Header>.toTarget(): Headers = listOf(*map { it.name to it.value }.toTypedArray())

    companion object {
        private fun defaultApacheHttpClient() = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setCookieSpec(IGNORE_COOKIES)
                .build()).build()

    }
}
