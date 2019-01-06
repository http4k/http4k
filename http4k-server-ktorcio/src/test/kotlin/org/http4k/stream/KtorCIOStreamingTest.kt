package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.KtorCIO
import org.http4k.streaming.StreamingContract
import org.http4k.streaming.StreamingTestConfiguration
import java.util.Random

class KtorCIOStreamingTest : StreamingContract(
    StreamingTestConfiguration(multiplier = 4)
) {
    override fun serverConfig() = KtorCIO(Random().nextInt(1000) + 8000)

    override fun createClient() = ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}