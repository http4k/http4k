package org.http4k.client

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.core5.util.Timeout
import org.http4k.core.BodyMode.Stream
import org.http4k.server.Jetty

class ApacheClientStreamingTest : HttpClientContract(
    { Jetty(it) },
    ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream),
    ApacheClient(
        org.apache.hc.client5.http.impl.classic.HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(100))
                    .build()
            ).build(),
        responseBodyMode = Stream,
        requestBodyMode = Stream
    )
)
