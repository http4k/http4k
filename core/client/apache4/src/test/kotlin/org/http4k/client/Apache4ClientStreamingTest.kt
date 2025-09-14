package org.http4k.client

import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.HttpClients
import org.http4k.core.BodyMode.Stream
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

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
) {
    @Test
    override fun `malformed response chunk is converted into 503`() = assumeTrue(false, "Unsupported feature")

    @Test
    override fun `random data then close is converted into 503`() = assumeTrue(false, "Unsupported feature")
}
