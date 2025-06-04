package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpNodeType
import java.time.Duration
import kotlin.random.Random

internal class ClientPrompts(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: org.http4k.ai.mcp.client.internal.McpRpcSender,
    private val random: Random,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Prompts {
    override fun onChange(fn: () -> Unit) {
        register(McpPrompt.List, McpCallback(McpPrompt.List.Changed.Notification::class) { _, _ ->
            fn()
        })
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpPrompt.List,
        McpPrompt.List.Request(), overrideDefaultTimeout ?: defaultTimeout, McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpPrompt.List.Response>() }
        .map { it.prompts }

    override fun get(name: PromptName, request: PromptRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpPrompt.Get,
            McpPrompt.Get.Request(name, request, request.meta),
            overrideDefaultTimeout ?: defaultTimeout,
            McpMessageId.random(random)
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
}
