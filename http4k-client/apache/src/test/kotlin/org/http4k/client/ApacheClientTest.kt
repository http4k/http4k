package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.apache.hc.client5.http.ConnectTimeoutException
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.util.Timeout
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.hamkrest.hasStatus
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test

class ApacheClientTest : HttpClientContract({ ApacheServer(it) }, ApacheClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(100))
                .build()
        ).build()
        , responseBodyMode = Stream)) {

    @Test
    fun `connect timeout is handled`() {
        assertThat(ApacheClient(object : CloseableHttpClient() {
            override fun doExecute(target: HttpHost?, request: ClassicHttpRequest?, context: HttpContext?): CloseableHttpResponse {
                throw ConnectTimeoutException("test timeout")
            }

            override fun close(closeMode: CloseMode?) {
            }

            override fun close() {
            }
        })(Request(GET, "http://localhost:8000")), hasStatus(CLIENT_TIMEOUT))
    }
}
