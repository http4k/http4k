package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.http4k.core.BodyMode.Stream
import org.http4k.server.Jetty
import org.junit.jupiter.api.Disabled

class JavaHttpClientTest : HttpClientContract({ Jetty(it) }, JavaHttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(100))
                .build()
        ).build()
        , responseBodyMode = Stream)) {

    @Disabled("unsupported by the underlying java client")
    override fun `handles response with custom status message`() {
        super.`handles response with custom status message`()
    }
}
