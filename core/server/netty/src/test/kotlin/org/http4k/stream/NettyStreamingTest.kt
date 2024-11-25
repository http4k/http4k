package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ClientForServerTesting
import org.http4k.server.Netty
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.streaming.StreamingContract
import org.junit.jupiter.api.Test

class NettyStreamingTest : StreamingContract() {
    override fun serverConfig() = Netty(0, Immediate)

    override fun createClient() =
        ClientForServerTesting(requestBodyMode = Stream, responseBodyMode = Stream)

    @Test
    override fun `can stream request`() {
        // request streaming is not currently supported in the Netty integration
    }
}
