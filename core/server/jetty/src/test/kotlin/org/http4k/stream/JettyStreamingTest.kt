package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.ClientForServerTesting
import org.http4k.server.Jetty
import org.http4k.server.ServerConfig
import org.http4k.streaming.StreamingContract

class JettyStreamingTest : StreamingContract() {
    override fun serverConfig() = Jetty(0, ServerConfig.StopMode.Immediate)

    override fun createClient(): HttpHandler =
        ClientForServerTesting(bodyMode = Stream)
}
