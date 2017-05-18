package org.http4k.client

import okhttp3.OkHttpClient
import okhttp3.RequestBody.create
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import java.nio.ByteBuffer.wrap

class OkHttp(private val client: OkHttpClient = OkHttpClient()) : HttpHandler {

    private fun Request.asOkHttp(): okhttp3.Request =
        headers.fold(okhttp3.Request.Builder()
            .url(uri.toString())
            .method(method.toString(), body?.let { create(null, it.payload.array()) })) {
            memo, (first, second) ->
            memo.addHeader(first, second)
        }.build()

    private fun okhttp3.Response.asHttp4k(): Response {
        val initial = body()?.let {
            Response(Status(code(), ""))
                .body(Body(wrap(it.bytes())))
        } ?: Response(Status(code(), ""))
        return headers().toMultimap().asSequence().fold(
            initial) { memo, headerValues ->
            headerValues.value.fold(memo) { memo2, headerValue ->
                memo2.header(headerValues.key, headerValue)
            }
        }
    }

    override fun invoke(request: Request): Response = client.newCall(request.asOkHttp()).execute().asHttp4k()
}
