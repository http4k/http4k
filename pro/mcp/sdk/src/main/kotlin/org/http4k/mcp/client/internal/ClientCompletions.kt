package org.http4k.mcp.client.internal

import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.sse.SseMessage

internal class ClientCompletions(
    private val queueFor: (RequestId) -> Iterable<SseMessage.Event>,
    private val sender: McpRpcSender
) : McpClient.Completions {
    override fun complete(request: CompletionRequest) =
        sender(McpCompletion, McpCompletion.Request(request.ref, request.argument)) { true }
            .mapCatching(queueFor).map { it.first().asAOrThrow<McpCompletion.Response>() }
            .map { CompletionResponse(it.completion) }
}
