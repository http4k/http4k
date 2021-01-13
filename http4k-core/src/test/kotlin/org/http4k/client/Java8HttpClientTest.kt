package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.http4k.core.BodyMode
import org.http4k.server.ApacheServer
import java.util.concurrent.TimeUnit.MILLISECONDS

class Java8HttpClientTest : HttpClientContract({ ApacheServer(it) }, Java8HttpClient(),
    ApacheClient(HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(100, MILLISECONDS).build()).build()
        , responseBodyMode = BodyMode.Stream))
