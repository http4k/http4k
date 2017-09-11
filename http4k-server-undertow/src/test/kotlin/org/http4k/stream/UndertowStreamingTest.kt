package org.http4k.stream

import org.http4k.client.ApacheClient
import org.http4k.client.ResponseBodyMode
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig
import org.http4k.server.Undertow
import org.http4k.streaming.StreamingContract

class UndertowStreamingTest : StreamingContract() {
    override fun serverConfig(port: Int): ServerConfig = Undertow(port)

    override fun createClient(): HttpHandler = ApacheClient(bodyMode = ResponseBodyMode.Stream)

}