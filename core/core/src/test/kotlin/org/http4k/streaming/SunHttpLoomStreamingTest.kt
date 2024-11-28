package org.http4k.streaming

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ClientForServerTesting
import org.http4k.server.SunHttpLoom

class SunHttpLoomStreamingTest : StreamingContract() {
    override fun serverConfig() = SunHttpLoom(0)

    override fun createClient() = ClientForServerTesting(bodyMode = Stream)
}
