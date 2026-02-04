package org.http4k.ai.mcp

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.llm.LLMError
import org.http4k.ai.llm.LLMError.Custom
import org.http4k.ai.llm.LLMError.Http
import org.http4k.ai.llm.LLMError.Internal
import org.http4k.ai.llm.LLMError.Timeout
import org.http4k.ai.llm.model.Message.ToolResult
import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.llm.tools.ToolResponse
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content

fun McpError.toLLM(): LLMError = when (this) {
    is McpError.Http -> Http(response)
    is McpError.Internal -> Internal(cause)
    is Protocol -> Custom(error)
    is McpError.Timeout -> Timeout
}

fun org.http4k.ai.mcp.ToolResponse.toLLM(request: ToolRequest) = when (this) {
    is Ok -> Success(
        ToolResponse(
            (content ?: emptyList())
                .filterIsInstance<Content.Text>()
                .map { ToolResult(request.id, request.name, it.text) }
                .first())
    )

    is Error -> Failure(Protocol(error).toLLM())
    else -> Failure(Custom("Response cannot be converted to LLM response"))
}
