/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.llm.tools.stateless

import org.http4k.ai.llm.tools.LLMTool
import org.http4k.ai.llm.tools.LLMToolsContract
import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.McpEntity
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.Version
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.ai.mcp.stateless.testing.testMcpClient
import org.http4k.routing.stateless.bind
import org.http4k.routing.stateless.mcp

class McpLLMToolsTest : LLMToolsContract {

    override val echoTool = LLMTool("echo", "echoes", mapOf(
        "type" to "object",
        "properties" to emptyMap<String, String>(),
        "required" to emptyList<String>()
    ))

    private val mcp = mcp(
        ServerMetaData(McpEntity.of("123"), Version.of("123")),
        NoMcpSecurity,
        Tool(echoTool.name.value, echoTool.description) bind {
            Ok(it["arg"].toString().reversed()) },
    )
    override val llmTools = McpLLMTools(mcp.http!!.testMcpClient())
}
