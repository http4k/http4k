package org.http4k.aws

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.http.async.AsyncExecuteRequest
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.utils.async.SimplePublisher
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class AwsSdkAsyncClient(private val http: HttpHandler) : SdkAsyncHttpClient {
    override fun close() {}

    override fun execute(request: AsyncExecuteRequest) = try {
        val response = http(request.toHttp4k())

        val publisher = SimplePublisher<ByteBuffer>()
        publisher.send(response.body.payload)

        with(request.responseHandler()) {
            onHeaders(response.awsHeaders())
            onStream(publisher)
        }

        CompletableFuture<Void>().also {
            publisher.complete().whenComplete { _, _ ->
                it.complete(null)
            }
        }
    } catch (e: Throwable) {
        CompletableFuture<Void>().also { it.completeExceptionally(e) }
    }
}

private fun AsyncExecuteRequest.toHttp4k() = with(request()) {
    val init = Request(Method.valueOf(method().name), Uri.of(uri.toString()))
        .headers(headers().entries.flatMap { (name, values) -> values.map { name to it } })

    when (this) {
        is SdkHttpFullRequest ->
            init.body(contentStreamProvider().map { Body(it.newStream()) }.orElse(Body.EMPTY))
        else -> init
    }
}

private fun Response.awsHeaders() = SdkHttpResponse.builder()
    .statusCode(status.code)
    .statusText(status.description)
    .headers(headers.groupBy { it.first }.mapValues { it.value.map { it.second } })
    .build()
