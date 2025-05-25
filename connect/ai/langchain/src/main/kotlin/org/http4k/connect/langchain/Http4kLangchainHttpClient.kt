package org.http4k.connect.langchain

import dev.langchain4j.http.client.HttpClient
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * A LangChain4k [HttpClient] implementation using an [HttpHandler].
 */
class Http4kLangchainHttpClient(private val http: HttpHandler) : HttpClient {
    override fun execute(request: HttpRequest) = http(request.asHttp4k()).fromHttp4k()

    override fun execute(request: HttpRequest, parser: ServerSentEventParser, listener: ServerSentEventListener) {
        parser.parse(http(request.asHttp4k()).body.stream, listener)
    }
}

private fun Response.fromHttp4k() =
    SuccessfulHttpResponse.builder().statusCode(status.code)
        .headers(headers.groupBy { it.first }
            .mapValues { it.value.map { it.second ?: "" } })
        .body(bodyString())
        .build()

private fun HttpRequest.asHttp4k() = Request(Method.valueOf(method().name), url().toString())
    .body(body())
    .headers(headers().entries.flatMap { (key, value) -> value.map { key to it } })
