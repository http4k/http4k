package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.Helidon
import org.http4k.server.ServerConfig
import org.http4k.streaming.StreamingContract

class HelidonStreamingTest : StreamingContract() {
    override fun serverConfig(): ServerConfig = Helidon(0)

    override fun createClient(): HttpHandler =
        ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}
