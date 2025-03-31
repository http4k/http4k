package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.util.McpNodeType
import java.time.Duration
import kotlin.random.Random

internal class ClientCompletions(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val random: Random
) : McpClient.Completions {
    override fun complete(ref: Reference, request: CompletionRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpCompletion, McpCompletion.Request(ref, request.argument),
            overrideDefaultTimeout ?: defaultTimeout,
            McpMessageId.random(random)
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpCompletion.Response>() }
            .map { CompletionResponse(it.completion.values, it.completion.total, it.completion.hasMore) }
}
