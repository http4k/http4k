package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.KtorNetty
import org.http4k.streaming.StreamingContract
import org.http4k.streaming.StreamingTestConfiguration
import org.junit.jupiter.api.BeforeEach
import java.util.Random

class KtorNettyStreamingTest : StreamingContract(
    StreamingTestConfiguration(multiplier = 4)
) {
    override fun serverConfig() = KtorNetty(Random().nextInt(1000) + 10000)

    override fun createClient() = ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}
