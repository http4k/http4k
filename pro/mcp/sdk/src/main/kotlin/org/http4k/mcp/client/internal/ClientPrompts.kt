package org.http4k.mcp.client.internal

import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpNodeType

internal class ClientPrompts(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val sender: McpRpcSender,
    private val register: (McpRpc, NotificationCallback<*>) -> Any
) : McpClient.Prompts {
    override fun onChange(fn: () -> Unit) {
        register(McpPrompt.List, NotificationCallback(McpPrompt.List.Changed.Notification::class) { fn() })
    }

    override fun list() = sender(McpPrompt.List, McpPrompt.List.Request()) { true }
        .mapCatching(queueFor)
        .map { it.first().asAOrThrow<McpPrompt.List.Response>() }
        .map { it.prompts }

    override fun get(name: String, request: PromptRequest) =
        sender(McpPrompt.Get, McpPrompt.Get.Request(name, request)) { true }
            .mapCatching(queueFor)
            .map { it.first().asAOrThrow<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
}
