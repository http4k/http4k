package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode
import org.http4k.server.Jetty

class JavaHttpClientTest : HttpClientContract({ Jetty(it) }, JavaHttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultSocketConfig(
            SocketConfig.custom()
                .setSoTimeout(100)
                .build()
        ).build()
        , responseBodyMode = BodyMode.Stream)) {
}
