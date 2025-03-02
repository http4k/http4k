package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpNodeType
import java.time.Duration

internal class ClientPrompts(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Prompts {
    override fun onChange(fn: () -> Unit) {
        register(McpPrompt.List, McpCallback(McpPrompt.List.Changed.Notification::class) { fn() })
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpPrompt.List,
        McpPrompt.List.Request(), overrideDefaultTimeout ?: defaultTimeout
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpPrompt.List.Response>() }
        .map { it.prompts }

    override fun get(name: PromptName, request: PromptRequest, overrideDefaultTimeout: Duration?) =
        sender(McpPrompt.Get, McpPrompt.Get.Request(name, request), overrideDefaultTimeout ?: defaultTimeout)
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
}
