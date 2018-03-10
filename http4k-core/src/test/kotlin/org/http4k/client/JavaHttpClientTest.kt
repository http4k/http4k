package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode
import org.http4k.server.SunHttp
import org.junit.Ignore


class JavaHttpClientTest : HttpClientContract({ SunHttp(it) }, JavaHttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultSocketConfig(
            SocketConfig.custom()
                .setSoTimeout(100)
                .build()
        ).build()
        , responseBodyMode = BodyMode.Stream)) {

    @Ignore
    override fun `can forward response body to another request`() {

    }
}
