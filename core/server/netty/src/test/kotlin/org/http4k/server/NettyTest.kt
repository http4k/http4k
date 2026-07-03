package org.http4k.server

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.REQUEST_ENTITY_TOO_LARGE
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.junit.jupiter.api.Test
import java.time.Duration.ofMillis

val defaultStopMode = Graceful(ofMillis(1))

class NettyTest : ServerContract({ port, _ -> Netty(port, defaultStopMode) }, ClientForServerTesting()) {

    @Test
    fun `sets keep-alive for non-streaming response`() {
        assertThat(client(Request(Method.GET, "$baseUrl/headers")),
            allOf(
                hasStatus(Status.ACCEPTED),
                hasHeader("connection", "keep-alive")
            )
        )
        assertThat(client(Request(Method.GET, "$baseUrl/stream")),
            allOf(
                hasStatus(Status.OK),
                hasHeader("connection", "close")
            )
        )
    }

    @Test
    fun `rejects a request body that exceeds the maximum size`() {
        val response = client(Request(POST, "$baseUrl/echo").body("x".repeat(MAX_REQUEST_SIZE + 1)))

        assertThat(response, hasStatus(REQUEST_ENTITY_TOO_LARGE))
    }
}
