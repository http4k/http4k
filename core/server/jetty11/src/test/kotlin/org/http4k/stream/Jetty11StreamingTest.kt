package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.ClientForServerTesting
import org.http4k.server.Jetty11
import org.http4k.streaming.StreamingContract
import org.http4k.testingStopMode

class Jetty11StreamingTest : StreamingContract() {
    override fun serverConfig() = Jetty11(0, testingStopMode)

    override fun createClient(): HttpHandler =
        ClientForServerTesting(bodyMode = Stream)
}
