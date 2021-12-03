package org.http4k.streaming

import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig

class MemoryStreamingTest : StreamingContract(StreamingTestConfiguration(5, 100, 100)) {
    override fun serverConfig(): ServerConfig = DummyServerConfig
    override fun createClient(): HttpHandler = app
}

object DummyServerConfig : ServerConfig {
    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        override fun port() = 0
        override fun start() = this
        override fun stop() = this
    }
}
