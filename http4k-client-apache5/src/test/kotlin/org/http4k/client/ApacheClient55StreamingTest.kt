package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.core5.util.Timeout
import org.http4k.core.BodyMode.Stream
import org.http4k.server.Jetty

class ApacheClient55StreamingTest : HttpClientContract({ Jetty(it) },
    ApacheClient5(requestBodyMode = Stream, responseBodyMode = Stream),
    ApacheClient5(org.apache.hc.client5.http.impl.classic.HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(100))
                .build()
        ).build(),
        responseBodyMode = Stream,
        requestBodyMode = Stream)
)
