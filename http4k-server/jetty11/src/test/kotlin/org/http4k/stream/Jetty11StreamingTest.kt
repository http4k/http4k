package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.Jetty11
import org.http4k.server.ServerConfig
import org.http4k.testingStopMode
import org.http4k.streaming.StreamingContract

class Jetty11StreamingTest : StreamingContract() {
    override fun serverConfig(): ServerConfig = Jetty11(0, testingStopMode)

    override fun createClient(): HttpHandler =
        ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}
