package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.util.McpNodeType
import java.time.Duration

internal class ClientCompletions(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
) : McpClient.Completions {
    override fun complete(request: CompletionRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpCompletion, McpCompletion.Request(request.ref, request.argument),
            overrideDefaultTimeout ?: defaultTimeout
        ) { true }
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpCompletion.Response>() }
            .map { CompletionResponse(it.completion) }
}
