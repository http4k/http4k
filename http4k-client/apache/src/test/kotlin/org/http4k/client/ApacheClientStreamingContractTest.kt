package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.server.Jetty
import org.http4k.server.ServerConfig
import org.http4k.streaming.StreamingContract
import org.http4k.streaming.StreamingTestConfiguration
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class ApacheClientStreamingContractTest : StreamingContract(StreamingTestConfiguration(multiplier = 5, debug = true)) {
    override fun serverConfig(): ServerConfig = Jetty(0)

    val client = ApacheClient(requestBodyMode = BodyMode.Stream, responseBodyMode = BodyMode.Stream)
    override fun createClient(): HttpHandler {
        return client
    }

    init {
        println("test started")
    }

    @Test
    fun `basic request`(){
        assertThat(client(Request(Method.GET, "$baseUrl/ping")), hasStatus(OK))
    }
}
