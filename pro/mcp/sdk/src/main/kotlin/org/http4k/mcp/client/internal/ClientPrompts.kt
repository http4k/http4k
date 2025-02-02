package org.http4k.mcp.client.internal

import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.sse.SseMessage

internal class ClientPrompts(
    private val queueFor: (RequestId) -> Iterable<SseMessage.Event>,
    private val sender: McpRpcSender,
    private val register: (NotificationCallback<*>) -> Any
) : McpClient.Prompts {
    override fun onChange(fn: () -> Unit) {
        register(
            NotificationCallback(
                McpPrompt.List.Changed,
                McpPrompt.List.Changed.Notification::class
            ) { fn() }
        )
    }

    override fun list() = sender(McpPrompt.List, McpPrompt.List.Request()) { true }
        .mapCatching(queueFor).map { it.first().asAOrThrow<McpPrompt.List.Response>() }
        .map { it.prompts }

    override fun get(name: String, request: PromptRequest) =
        sender(McpPrompt.Get, McpPrompt.Get.Request(name, request)) { true }
            .mapCatching(queueFor).map { it.first().asAOrThrow<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
}
