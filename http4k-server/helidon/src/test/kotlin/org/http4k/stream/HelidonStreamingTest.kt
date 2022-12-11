package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.Helidon
import org.http4k.streaming.StreamingContract
import org.junit.jupiter.api.Disabled

@Disabled
class HelidonStreamingTest : StreamingContract() {
    override fun serverConfig() = Helidon(0)

    override fun createClient() =
        ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}
