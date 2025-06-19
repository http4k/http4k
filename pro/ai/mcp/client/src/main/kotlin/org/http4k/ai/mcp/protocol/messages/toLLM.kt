package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.llm.tools.LLMTool

fun McpTool.toLLM() = LLMTool(name, description, inputSchema)
