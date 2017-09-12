package org.http4k.client

import okhttp3.OkHttpClient
import okhttp3.RequestBody.create
import okhttp3.internal.http.HttpMethod.permitsRequestBody
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class OkHttp(private val client: OkHttpClient = defaultOkHttpClient(), private val bodyMode: ResponseBodyMode = ResponseBodyMode.Memory) : HttpHandler {

    private fun Request.asOkHttp(): okhttp3.Request =
        headers.fold(okhttp3.Request.Builder()
            .url(uri.toString())
            .method(method.toString(), requestBody())) { memo, (first, second) ->
            memo.addHeader(first, second)
        }.build()

    private fun Request.requestBody() =
        if (permitsRequestBody(method.toString())) create(null, body.payload.array())
        else null

    private fun okhttp3.Response.asHttp4k(): Response {
        val initial = body()?.let {
            Response(Status(code(), ""))
                .body(bodyMode(it.byteStream()))
        } ?: Response(Status(code(), ""))
        return headers().toMultimap().asSequence().fold(
            initial) { memo, headerValues ->
            headerValues.value.fold(memo) { memo2, headerValue ->
                memo2.header(headerValues.key, headerValue)
            }
        }
    }

    override fun invoke(request: Request): Response = client.newCall(request.asOkHttp()).execute().asHttp4k()

    companion object {
        private fun defaultOkHttpClient() = OkHttpClient.Builder()
            .followRedirects(false)
            .build()
    }
}
