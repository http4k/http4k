package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode.Stream
import org.http4k.server.Jetty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ApacheClientStreamingTest : HttpClientContract({ Jetty(it) },
    ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream),
    ApacheClient(HttpClients.custom()
        .setDefaultSocketConfig(
            SocketConfig.custom()
                .setSoTimeout(100)
                .build()
        ).build(),
        responseBodyMode = Stream,
        requestBodyMode = Stream)
){
        @Test
        @Disabled
        override fun `handles response with custom status message`(){}
}
