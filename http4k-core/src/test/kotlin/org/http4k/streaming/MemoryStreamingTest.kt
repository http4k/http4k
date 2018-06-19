package org.http4k.streaming

import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig

class MemoryStreamingTest : StreamingContract(StreamingTestConfiguration(5, 100, 100)) {
    override fun serverConfig(port: Int): ServerConfig = DummyServerConfig
    override fun createClient(): HttpHandler = server
}

object DummyServerConfig : ServerConfig {
    override fun toServer(httpHandler: HttpHandler): Http4kServer = object : Http4kServer {
        override fun port(): Int = -1
        override fun start(): Http4kServer = this
        override fun stop() = Unit
    }
}