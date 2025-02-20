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

internal class ClientPrompts(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val sender: McpRpcSender,
    private val register: (McpRpc, NotificationCallback<*>) -> Any
) : McpClient.Prompts {
    override fun onChange(fn: () -> Unit) {
        register(McpPrompt.List, NotificationCallback(McpPrompt.List.Changed.Notification::class) { fn() })
    }

    override fun list() = sender(McpPrompt.List, McpPrompt.List.Request()) { true }
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpPrompt.List.Response>() }
        .map { it.prompts }

    override fun get(name: PromptName, request: PromptRequest) =
        sender(McpPrompt.Get, McpPrompt.Get.Request(name, request)) { true }
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
}
