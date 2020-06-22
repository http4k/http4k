package org.http4k.client

import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.http4k.core.BodyMode
import org.http4k.server.SunHttp

class ApacheAsyncClientStreamingTest : AsyncHttpClientContract({ SunHttp(it) },
    ApacheAsyncClient(responseBodyMode = BodyMode.Stream),
    ApacheAsyncClient(HttpAsyncClients.custom()
        .setDefaultIOReactorConfig(IOReactorConfig.custom()
            .setConnectTimeout(100)
            .setSoTimeout(100)
            .build()).build().apply { start() },
        responseBodyMode = BodyMode.Stream,
        requestBodyMode = BodyMode.Stream)
)
