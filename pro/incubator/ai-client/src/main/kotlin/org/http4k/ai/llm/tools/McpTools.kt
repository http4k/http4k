package org.http4k.ai.llm.tools

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.LLMResult
import org.http4k.ai.llm.model.Message.ToolResult
import org.http4k.ai.mcp.McpError.Internal
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Content

/**
 * Tools implementation for the MCP protocol.
 */
class McpTools(private val client: McpClient) : Tools {
    override fun list() = client.tools().list()
        .map { it.map { LLMTool(it.name, it.description, it.inputSchema) } }
        .mapFailure { LLMError.Internal(java.lang.Exception(it.toString())) }

    override fun invoke(request: ToolRequest): LLMResult<ToolResponse> =
        client.tools().call(request.name, org.http4k.ai.mcp.ToolRequest(request.arguments))
            .flatMap {
                when (it) {
                    is Ok -> when {
                        it.content?.isNotEmpty() == true ->
                            Success(ToolResponse(it.getContent(request)))

                        else -> Failure(Internal(Exception(it.toString())))
                    }

                    is Error -> Failure(Protocol(it.error))
                }
            }
            .mapFailure { LLMError.Internal(java.lang.Exception(it.toString())) }
}

private fun Ok.getContent(request: ToolRequest) = (content ?: emptyList())
    .filterIsInstance<Content.Text>()
    .map { ToolResult(request.id, request.name, it.text) }
    .first()
