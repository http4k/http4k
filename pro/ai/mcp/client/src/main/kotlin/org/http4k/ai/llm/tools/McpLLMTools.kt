package org.http4k.ai.llm.tools

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.toLLM
import org.http4k.ai.mcp.toLLM
import org.http4k.ai.mcp.ToolRequest as McpToolRequest

/**
 * Tools implementation for the MCP protocol.
 */
class McpLLMTools(private val client: McpClient) : LLMTools {
    override fun list() = client.tools().list()
        .map { it.map(McpTool::toLLM) }
        .mapFailure { LLMError.Internal(Exception(it.toString())) }

    override fun invoke(request: ToolRequest): LLMResult<ToolResponse> =
        client.tools()
            .call(request.name, McpToolRequest(request.arguments, meta = Meta(request.id.value)))
            .mapFailure { it.toLLM() }
            .flatMap {
                when (it) {
                    is Ok -> when {
                        it.content?.isNotEmpty() == true -> it.toLLM(request)
                        else -> it.toLLM(request)
                    }

                    else -> it.toLLM(request)
                }
            }
}
