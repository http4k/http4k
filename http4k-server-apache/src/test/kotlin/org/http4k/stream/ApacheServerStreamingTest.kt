package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.server.ApacheServer
import org.http4k.server.ServerConfig
import org.http4k.streaming.StreamingContract

class ApacheServerStreamingTest : StreamingContract() {
    override fun serverConfig(port: Int): ServerConfig = ApacheServer(port)

    override fun createClient(): HttpHandler =
            ApacheClient(requestBodyMode = BodyMode.Stream, responseBodyMode = BodyMode.Stream)

}