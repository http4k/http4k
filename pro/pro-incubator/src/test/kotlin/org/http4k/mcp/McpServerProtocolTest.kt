package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.debug
import org.http4k.format.renderNotification
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.hamkrest.hasStatus
import org.http4k.mcp.protocol.ClientMessage
import org.http4k.mcp.protocol.HasMethod
import org.http4k.mcp.protocol.McpInitialize
import org.http4k.mcp.protocol.McpRequest
import org.http4k.mcp.protocol.McpResponse
import org.http4k.mcp.protocol.McpRoot
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.ClientCapabilities
import org.http4k.mcp.server.McpEntity
import org.http4k.mcp.server.McpHandler
import org.http4k.mcp.server.ServerMetaData
import org.http4k.mcp.testing.jsonRpcResult
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.random.Random

@ExtendWith(JsonApprovalTest::class)
class McpServerProtocolTest {

    private val metadata = ServerMetaData(McpEntity("server", Version.of("1")))

    @Test
    fun `performs init loop on startup`() {
        val mcp = McpHandler(metadata, random = Random(0)).debug()

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)
        }
    }

    @Test
    fun `performs update roots`() {
        val mcp = McpHandler(metadata, random = Random(0)).debug()

        with(mcp.testSseClient(Request(GET, "/sse"))) {
            assertInitializeLoop(mcp)

            mcp.sendToMcp(McpRoot.Changed)

//            assertNextMessage(McpRoot.List, McpRoot.List.Request())
        }
    }

    private fun TestSseClient.assertInitializeLoop(mcp: PolyHandler) {
        assertThat(status, equalTo(OK))

        assertThat(
            received().first(),
            equalTo(SseMessage.Event("endpoint", "/message?sessionId=8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e"))
        )

        mcp.sendToMcp(
            McpInitialize, McpInitialize.Request(
                McpEntity("client", Version.of("1")),
                ClientCapabilities(), `2024-10-07`
            )
        )

        assertNextMessage(
            McpInitialize.Response(metadata.entity, metadata.capabilities, metadata.protocolVersion)
        )

        mcp.sendToMcp(McpInitialize, McpInitialize.Initialized)
    }

    private fun TestSseClient.assertNextMessage(input: McpResponse) {
        assertNextMessage(with(McpJson) { renderResult(asJsonObject(input), number(1)) })
    }

    private fun TestSseClient.assertNextMessage(hasMethod: HasMethod, input: McpRequest) {
        assertNextMessage(with(McpJson) { renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)) })
    }

    private fun TestSseClient.assertNextMessage(node: JsonNode) {
        assertThat(
            received().first(),
            equalTo(
                SseMessage.Event("message",
                    with(McpJson) {
                        compact(
                            node
                        )
                    })
            )
        )
    }

    private fun PolyHandler.sendToMcp(hasMethod: HasMethod, input: ClientMessage.Request) {
        assertThat(
            http!!(
                Request(POST, "/message?sessionId=8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")
                    .body(
                        with(McpJson) {
                            compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
                        }
                    )

            ), hasStatus(ACCEPTED)
        )
    }

    private fun PolyHandler.sendToMcp(hasMethod: HasMethod) {
        assertThat(
            http!!(
                Request(POST, "/message?sessionId=8cb4c22c-53fe-ae50-d94e-97b2a94e6b1e")
                    .body(
                        with(McpJson) {
                            compact(renderNotification(hasMethod.Method.value))
                        }
                    )

            ), hasStatus(ACCEPTED)
        )
    }

    private fun Sequence<SseMessage>.takeNextResult() =
        McpJson.jsonRpcResult(first() as SseMessage.Event)

}
