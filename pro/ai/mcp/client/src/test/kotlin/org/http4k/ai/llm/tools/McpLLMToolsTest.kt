package org.http4k.ai.llm.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming

class McpLLMToolsTest : LLMToolsContract {

    override val echoTool = LLMTool("echo", "echoes", mapOf(
        "type" to "object",
        "properties" to emptyMap<String, String>(),
        "required" to emptyList<String>()
    ))

    private val mcp = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("123"), Version.of("123")),
        NoMcpSecurity,
        Tool(echoTool.name.value, echoTool.description) bind {
            Ok(it["arg"].toString().reversed()) },
    )
    override val llmTools = McpLLMTools(mcp.testMcpClient())

}
