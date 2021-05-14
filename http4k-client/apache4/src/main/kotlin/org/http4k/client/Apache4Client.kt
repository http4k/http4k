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
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.conn.HttpHostConnectException
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.InputStreamEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContextBuilder
import org.http4k.client.PreCannedApache4HttpClients.defaultApacheHttpClient
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

object Apache4Client {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
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

    private fun Request.toApacheRequest(requestBodyMode: BodyMode): HttpRequestBase {
        val request = this@toApacheRequest
        val uri = URI(request.uri.toString())

        val apacheRequest = when (method) {
            HEAD -> HttpHead(uri)
            GET -> HttpGet(uri)
            OPTIONS -> HttpOptions(uri)
            TRACE -> HttpTrace(uri)
            DELETE -> HttpDelete(uri)
            else -> Apache4Request(requestBodyMode, request)
        }
        request.headers.filter { !it.first.equals("content-length", true) }.map { apacheRequest.addHeader(it.first, it.second) }
        return apacheRequest
    }

    private fun StatusLine.toTarget() = Status(statusCode, reasonPhrase)

    private fun Array<Header>.toTarget(): Headers = map { it.name to it.value }

    private fun CloseableHttpResponse.toHttp4kResponse(responseBodyMode: BodyMode) = with(Response(statusLine.toTarget()).headers(allHeaders.toTarget())) {
        entity?.let { body(responseBodyMode(it.content)) } ?: this
    }
}

private class Apache4Request(requestBodyMode: BodyMode, private val request: Request) : HttpEntityEnclosingRequestBase() {
    init {
        uri = URI(request.uri.toString())
        entity = when (requestBodyMode) {
            Stream -> InputStreamEntity(request.body.stream, request.header("content-length")?.toLong() ?: -1)
            Memory -> ByteArrayEntity(request.body.payload.array())
        }
    }

    override fun getMethod() = request.method.name
}

object PreCannedApache4HttpClients {

    /**
     * Standard non-redirecting, no Cookies HTTP client
     */
    fun defaultApacheHttpClient() = HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setRedirectsEnabled(false)
            .setCookieSpec(IGNORE_COOKIES)
            .build()).build()

    /**
     * Do not use this in production! This is useful for testing locally and debugging HTTPS traffic
     */
    fun insecureApacheHttpClient() = SSLContextBuilder()
        .loadTrustMaterial(null) { _, _ -> true }
        .build().run {
            HttpClientBuilder.create()
                .setSSLContext(this)
                .setConnectionManager(
                    PoolingHttpClientConnectionManager(
                        RegistryBuilder.create<ConnectionSocketFactory>()
                            .register("http", PlainConnectionSocketFactory.INSTANCE)
                            .register("https", SSLConnectionSocketFactory(this) { _, _ -> true })
                            .build()
                    ))
                .build()
        }
}
