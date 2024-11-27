package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode.Stream
import org.http4k.server.ApacheServer

class Apache4ClientStreamingTest : HttpClientContract(
    ::ApacheServer,
    Apache4Client(requestBodyMode = Stream, responseBodyMode = Stream),
    Apache4Client(
        HttpClients.custom()
            .setDefaultSocketConfig(
                SocketConfig.custom()
                    .setSoTimeout(100)
                    .build()
            ).build(),
        responseBodyMode = Stream,
        requestBodyMode = Stream
    )
)
