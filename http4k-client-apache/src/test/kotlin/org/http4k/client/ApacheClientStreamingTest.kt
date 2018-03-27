package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode.Stream
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.server.SunHttp

class ApacheClientStreamingTest : HttpClientContract({ SunHttp(it) },
    DebuggingFilters.PrintRequestAndResponse().then(ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)),
    ApacheClient(HttpClients.custom()
        .setDefaultSocketConfig(
            SocketConfig.custom()
                .setSoTimeout(100)
                .build()
        ).build(),
        responseBodyMode = Stream,
        requestBodyMode = Stream)
)
