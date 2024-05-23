package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.Jetty11Loom
import org.http4k.testingStopMode
import org.http4k.streaming.StreamingContract
import org.junit.jupiter.api.Disabled

@Disabled("temporarily disabled")
class Jetty11LoomStreamingTest : StreamingContract() {
    override fun serverConfig() = Jetty11Loom(0, testingStopMode)

    override fun createClient() =
        ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}
