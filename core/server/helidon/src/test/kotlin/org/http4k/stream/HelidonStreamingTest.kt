package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ClientForServerTesting
import org.http4k.server.Helidon
import org.http4k.streaming.StreamingContract

class HelidonStreamingTest : StreamingContract() {
    override fun serverConfig() = Helidon(0)

    override fun createClient() =
        ClientForServerTesting(bodyMode = Stream)
}
