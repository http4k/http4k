package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.http4k.core.BodyMode
import org.http4k.server.Jetty
import java.util.concurrent.TimeUnit.MILLISECONDS

class JavaHttpClientTest : HttpClientContract({ Jetty(it) }, JavaHttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(100, MILLISECONDS).build()).build()
        , responseBodyMode = BodyMode.Stream)) {
}
