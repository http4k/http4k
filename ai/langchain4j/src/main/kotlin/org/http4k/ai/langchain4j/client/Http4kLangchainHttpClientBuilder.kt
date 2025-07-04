package org.http4k.ai.langchain4j.client

import dev.langchain4j.http.client.HttpClientBuilder
import org.http4k.core.HttpHandler
import java.time.Duration

/**
 * A builder for creating a LangChain4k [HttpClient] using an [HttpHandler].
 */
class Http4kLangchainHttpClientBuilder(private val http: HttpHandler) : HttpClientBuilder {
    private var connectTimeout: Duration? = null
    private var readTimeout: Duration? = null
    override fun connectTimeout() = connectTimeout

    override fun connectTimeout(timeout: Duration) = apply {
        connectTimeout = timeout
    }

    override fun readTimeout() = readTimeout

    override fun readTimeout(timeout: Duration) = apply {
        readTimeout = timeout
    }

    override fun build() = Http4kLangchainHttpClient(http)
}

fun HttpHandler.asLangchainHttpClientBuilder() = Http4kLangchainHttpClientBuilder(this)
