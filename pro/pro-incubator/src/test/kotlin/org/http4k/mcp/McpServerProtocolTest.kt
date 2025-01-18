package org.http4k.mcp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.McpEntity
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.ServerMetaData
import org.http4k.mcp.testing.jsonRpcResult
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(JsonApprovalTest::class)
class McpServerProtocolTest {

    private val metadata = ServerMetaData(McpEntity("server", Version.of("1")))

    @Test
    fun `reports endpoint on startup`() {
        val mcp = McpHandler(metadata, random = Random(0))

        val client = mcp.testSseClient(Request(GET, "/sse"))
        assertThat(client.status, equalTo(OK))

        val events = client.received()

        assertThat(
            events.first(),
            equalTo(SseMessage.Event("endpoint", "/message?sessionId=8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e"))
        )
    }

    private fun Sequence<SseMessage>.takeNextResult() =
        McpJson.jsonRpcResult(first() as SseMessage.Event)

}
