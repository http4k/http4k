package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ClientForServerTesting
import org.http4k.server.Jetty11Loom
import org.http4k.streaming.StreamingContract
import org.http4k.testingStopMode

class Jetty11LoomStreamingTest : StreamingContract() {
    override fun serverConfig() = Jetty11Loom(0, testingStopMode)

    override fun createClient() =
        ClientForServerTesting(bodyMode = Stream)
}
