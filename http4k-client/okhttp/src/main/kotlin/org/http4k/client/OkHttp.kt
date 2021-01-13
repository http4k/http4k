package org.http4k.client

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HttpMethod.permitsRequestBody
import org.http4k.client.PreCannedOkHttpClients.defaultOkHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONNECTION_REFUSED
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.Status.Companion.UNKNOWN_HOST
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object OkHttp {
    @JvmStatic
    @JvmOverloads
    @JvmName("create")
    operator fun invoke(
        client: OkHttpClient = defaultOkHttpClient(),
        bodyMode: BodyMode = BodyMode.Memory
    ): DualSyncAsyncHttpHandler =
        object : DualSyncAsyncHttpHandler {
            override fun invoke(request: Request): Response =
                try {
                    client.newCall(request.asOkHttp()).execute().asHttp4k(bodyMode)
                } catch (e: ConnectException) {
                    Response(CONNECTION_REFUSED.toClientStatus(e))
                } catch (e: UnknownHostException) {
                    Response(UNKNOWN_HOST.toClientStatus(e))
                } catch (e: SocketTimeoutException) {
                    Response(CLIENT_TIMEOUT.toClientStatus(e))
                }

            override operator fun invoke(request: Request, fn: (Response) -> Unit) =
                client.newCall(request.asOkHttp()).enqueue(Http4kCallback(bodyMode, fn))
        }

    private class Http4kCallback(private val bodyMode: BodyMode, private val fn: (Response) -> Unit) : Callback {
        override fun onFailure(call: Call, e: IOException) = fn(
            Response(
                when (e) {
                    is SocketTimeoutException -> CLIENT_TIMEOUT
                    else -> SERVICE_UNAVAILABLE
                }.description("Client Error: caused by ${e.localizedMessage}")
            )
        )

        override fun onResponse(call: Call, response: okhttp3.Response) = fn(response.asHttp4k(bodyMode))
    }
}

private fun Request.asOkHttp(): okhttp3.Request = headers.fold(
    okhttp3.Request.Builder()
        .url(uri.toString())
        .method(method.toString(), requestBody())
) { memo, (first, second) ->
    val notNullValue = second ?: ""
    memo.addHeader(first, notNullValue)
}.build()

private fun Request.requestBody() =
    if (permitsRequestBody(method.toString())) body.payload.array().toRequestBody()
    else null

private fun okhttp3.Response.asHttp4k(bodyMode: BodyMode): Response {
    val init = Response(Status(code, message))
    val headers = headers.toMultimap().flatMap { it.value.map { hValue -> it.key to hValue } }

    return (body?.let { init.body(bodyMode(it.byteStream())) } ?: init).headers(headers)
}

object PreCannedOkHttpClients {

    /**
     * Standard non-redirecting client
     */
    fun defaultOkHttpClient() = OkHttpClient.Builder()
        .followRedirects(false)
        .build()

    /**
     * Do not use this in production! This is useful for testing locally and debugging HTTPS traffic
     */
    fun insecureOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
        })

        val sslSocketFactory = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }.build()
    }
}
