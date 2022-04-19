package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.config.SocketConfig
import org.apache.http.conn.ConnectTimeoutException
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.hamkrest.hasStatus
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test

class Apache4ClientTest : HttpClientContract(
    ::ApacheServer, Apache4Client(),
    Apache4Client(
        HttpClients.custom()
            .setDefaultSocketConfig(
                SocketConfig.custom()
                    .setSoTimeout(100)
                    .build()
            ).build(), responseBodyMode = Stream
    )
) {

    @Test
    fun `connect timeout is handled`() {
        assertThat(Apache4Client(object : CloseableHttpClient() {
            @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"not implemented\")"))
            override fun getParams() = TODO("not implemented")

            @Deprecated("Deprecated in Java", ReplaceWith("TODO(\"not implemented\")"))
            override fun getConnectionManager() = TODO("not implemented")

            override fun doExecute(
                target: HttpHost?,
                request: HttpRequest?,
                context: HttpContext?
            ): CloseableHttpResponse {
                throw ConnectTimeoutException()
            }

            override fun close() {
            }
        })(Request(GET, "http://localhost:8000")), hasStatus(CLIENT_TIMEOUT))
    }
}
