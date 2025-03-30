package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.protocol.messages.McpCompletion
import java.time.Duration

class TestingCompletions(private val send: TestMcpSender) : McpClient.Completions {
    override fun complete(
        request: CompletionRequest,
        overrideDefaultTimeout: Duration?
    ) = send(McpCompletion, McpCompletion.Request(request.ref, request.argument))
        .nextEvent<McpCompletion.Response, CompletionResponse>(
            { CompletionResponse(completion.values, completion.total, completion.hasMore) }
        ).map { it.second }
}
