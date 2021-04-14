package org.http4k.client

import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.streaming.StreamingContract
import org.http4k.streaming.StreamingTestConfiguration

class ApacheClientStreamingContractTest : StreamingContract(StreamingTestConfiguration(multiplier = 5, debug = true)) {
    override fun serverConfig(): ServerConfig = SunHttp(0)

    override fun createClient(): HttpHandler = ApacheClient(requestBodyMode = BodyMode.Stream, responseBodyMode = BodyMode.Stream)

init {
println("test started")
}
}
