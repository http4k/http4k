

package org.http4k.client

import okhttp3.OkHttpClient
import okhttp3.RequestBody.create
import okhttp3.internal.http.HttpMethod.permitsRequestBody
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.net.SocketTimeoutException

class OkHttp(private val client: OkHttpClient = defaultOkHttpClient(), private val bodyMode: BodyMode = BodyMode.Memory) : HttpHandler {

    private fun Request.asOkHttp(): okhttp3.Request =
        headers.fold(okhttp3.Request.Builder()
            .url(uri.toString())
            .method(method.toString(), requestBody())) { memo, (first, second) ->
            memo.addHeader(first, second ?: "")
        }.build()

    private fun Request.requestBody() =
        if (permitsRequestBody(method.toString())) create(null, body.payload.array())
        else null

    private fun okhttp3.Response.asHttp4k(): Response {
        val init = Response(Status(code(), ""))
        val headers = headers().toMultimap().flatMap { it.value.map { hValue -> it.key to hValue } }

        return (body()?.let { init.body(bodyMode(it.byteStream())) } ?: init)
            .headers(headers)
    }

    override fun invoke(request: Request): Response =
        try {
            client.newCall(request.asOkHttp()).execute().asHttp4k()
        } catch (e: SocketTimeoutException) {
            Response(Status.CLIENT_TIMEOUT)
        }

    companion object {
        private fun defaultOkHttpClient() = OkHttpClient.Builder()
            .followRedirects(false)
            .build()
    }
}
