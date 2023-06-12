package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.JettyLoom
import org.http4k.streaming.StreamingContract

class JettyLoomStreamingTest : StreamingContract() {
    override fun serverConfig() = JettyLoom(0)

    override fun createClient() =
        ApacheClient(requestBodyMode = Stream, responseBodyMode = Stream)
}
