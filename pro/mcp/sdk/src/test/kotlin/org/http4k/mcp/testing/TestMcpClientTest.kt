package org.http4k.mcp.testing

import org.http4k.filter.debug
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpSse
import org.junit.jupiter.api.Test

class TestMcpClientTest {

    @Test
    fun `can use mcp client to connect and get responses`() {

        val mcp = mcpSse(ServerMetaData(McpEntity.of("my mcp server"), Version.of("1"))).debug()

        val testMcpClient = mcp.testMcpClient()

        println(testMcpClient.start())
    }
}
