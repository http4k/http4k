package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import java.time.Duration

class TestingCompletions(
    private val sender: TestMcpSender,
) : McpClient.Completions {
    override fun complete(
        ref: Reference,
        request: CompletionRequest,
        overrideDefaultTimeout: Duration?
    ): Result<CompletionResponse, McpError> =
        sender(McpCompletion, McpCompletion.Request(ref, request.argument, request.context, request.meta))
            .first()
            .nextEvent<CompletionResponse, McpCompletion.Response> {
                CompletionResponse(completion.values, completion.total, completion.hasMore)
            }
            .map { it.second }

}
