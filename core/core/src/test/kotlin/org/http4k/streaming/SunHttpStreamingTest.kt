package org.http4k.streaming

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ClientForServerTesting
import org.http4k.server.SunHttp

class SunHttpStreamingTest : StreamingContract() {
    override fun serverConfig() = SunHttp(0)

    override fun createClient() = ClientForServerTesting(bodyMode = Stream)
}
