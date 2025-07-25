package org.http4k.client

import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.HttpHostConnectException
import org.apache.hc.client5.http.classic.methods.HttpDelete
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpHead
import org.apache.hc.client5.http.classic.methods.HttpOptions
import org.apache.hc.client5.http.classic.methods.HttpTrace
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec.IGNORE
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.NoHttpResponseException
import org.apache.hc.core5.http.io.entity.ByteArrayEntity
import org.apache.hc.core5.http.io.entity.InputStreamEntity
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
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

object ApacheClient {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        client: CloseableHttpClient = PreCannedApacheHttpClients.defaultApacheHttpClient(),
        responseBodyMode: BodyMode = Memory,
        requestBodyMode: BodyMode = Memory
    ): HttpHandler = { request ->
        try {
            when (responseBodyMode) {
                Memory -> client.execute(request.toApacheRequest(requestBodyMode)) {
                    it.toHttp4kResponse(responseBodyMode)
                }

                Stream -> client.executeOpen(null, request.toApacheRequest(requestBodyMode), null)
                    .toHttp4kResponse(responseBodyMode)
            }
        } catch (e: ConnectTimeoutException) {
            Response(CLIENT_TIMEOUT.toClientStatus(e))
        } catch (e: SocketTimeoutException) {
            Response(CLIENT_TIMEOUT.toClientStatus(e))
        } catch (e: HttpHostConnectException) {
            Response(CONNECTION_REFUSED.toClientStatus(e))
        } catch (e: UnknownHostException) {
            Response(UNKNOWN_HOST.toClientStatus(e))
        } catch (e: SocketException) {
            Response(SERVICE_UNAVAILABLE.toClientStatus(e))
        } catch (e: NoHttpResponseException) {
            Response(SERVICE_UNAVAILABLE.toClientStatus(e))
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
        request.headers.filter { !it.first.equals("content-length", true) }
            .map { apacheRequest.addHeader(it.first, it.second) }
        return apacheRequest
    }

    private fun HttpResponse.toTargetStatus() = Status(code, reasonPhrase)

    private fun Array<Header>.toTarget(): Headers = map { it.name to it.value }

    private fun ClassicHttpResponse.toHttp4kResponse(responseBodyMode: BodyMode) =
        with(Response(toTargetStatus()).headers(headers.toTarget())) {
            entity?.let { body(responseBodyMode(it.content)) } ?: this
        }
}

private class ApacheRequest(requestBodyMode: BodyMode, private val request: Request) :
    HttpUriRequestBase(request.method.toString(), URI(request.uri.toString())) {
    init {
        entity = when (requestBodyMode) {
            Stream -> InputStreamEntity(
                request.body.stream,
                request.body.length ?: request.header("content-length")?.toLong() ?: -1,
                null
            )

            Memory -> ByteArrayEntity(request.body.payload.array(), null)
        }
    }

    override fun getMethod() = request.method.name
}

object PreCannedApacheHttpClients {
    /**
     * Standard non-redirecting, no Cookies HTTP client
     */
    fun defaultApacheHttpClient(): CloseableHttpClient = HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setRedirectsEnabled(false)
                .setCookieSpec(IGNORE)
                .build()
        ).build()
}
