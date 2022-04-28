package org.http4k.server

import com.natpryce.hamkrest.allOf
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class NettyTest : ServerContract(::Netty, ApacheClient()) {
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
}
