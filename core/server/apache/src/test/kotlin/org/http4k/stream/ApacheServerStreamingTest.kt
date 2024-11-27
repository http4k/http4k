package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.ApacheServer
import org.http4k.server.ClientForServerTesting
import org.http4k.server.ServerConfig
import org.http4k.streaming.StreamingContract

class ApacheServerStreamingTest : StreamingContract() {
    override fun serverConfig(): ServerConfig = ApacheServer(0, canonicalHostname = "localhost")

    override fun createClient(): HttpHandler =
        ClientForServerTesting(requestBodyMode = Stream, responseBodyMode = Stream)
}
