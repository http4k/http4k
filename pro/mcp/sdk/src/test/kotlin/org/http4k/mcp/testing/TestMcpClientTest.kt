package org.http4k.mcp.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.core.PolyHandler
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ProtocolCapability.Experimental
import org.http4k.mcp.protocol.ProtocolCapability.PromptsChanged
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.ServerCapabilities.PromptCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpSse
import org.junit.jupiter.api.Test

class TestMcpClientTest {

    @Test
    fun `can use mcp client to connect and get responses`() {
        val capabilities = mcpSse(
            ServerMetaData(
                McpEntity.of("my mcp server"), Version.of("1"),
                PromptsChanged,
                Experimental,
            ),
        )
            .testMcpClient().start()

        assertThat(capabilities, equalTo(Success(ServerCapabilities(
            prompts = PromptCapabilities(true),
            experimental = Unit)))
        )
    }

    private fun PolyHandler.useClient(fn: McpClient.() -> Unit) {
        testMcpClient().use {
            it.start()
            it.fn()
        }
    }
}
