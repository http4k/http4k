package org.http4k.aws

import org.http4k.core.Body
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import software.amazon.awssdk.http.AbortableInputStream.create
import software.amazon.awssdk.http.ExecutableHttpRequest
import software.amazon.awssdk.http.HttpExecuteRequest
import software.amazon.awssdk.http.HttpExecuteResponse
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpFullResponse.builder

class AwsSdkClient(private val http: HttpHandler) : SdkHttpClient {
    override fun close() {
    }

    override fun prepareRequest(request: HttpExecuteRequest) = object : ExecutableHttpRequest {
        override fun call() = http(request.fromAws()).asAws()
        override fun abort() {}
    }
}

private fun HttpExecuteRequest.fromAws() = with(httpRequest()) {
    val init = Request(Method.valueOf(method().name), Uri.of(uri.toString()))
        .headers(headers().entries.flatMap { (name, values) -> values.map { name to it } })

    when (this) {
        is SdkHttpFullRequest ->
            init.body(contentStreamProvider().map { Body(it.newStream()) }.orElse(EMPTY))
        else -> init
    }
}

private fun Response.asAws() = HttpExecuteResponse.builder()
    .response(builder()
        .statusCode(status.code)
        .statusText(status.description)
        .headers(headers.groupBy { it.first }.mapValues { it.value.map { it.second } })
        .content(create(body.stream))
        .build())
    .build()
