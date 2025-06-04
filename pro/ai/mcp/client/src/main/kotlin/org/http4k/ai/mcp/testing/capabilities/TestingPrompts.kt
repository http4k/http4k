package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.nextNotification
import java.time.Duration

class TestingPrompts(
    private val sender: TestMcpSender,
) : McpClient.Prompts {
    private val notifications = mutableListOf<() -> Unit>()

    override fun onChange(fn: () -> Unit) {
        notifications += fn
    }

    /**
     * Expected a list changed notification to be received and process it
     */
    fun expectNotification() {
        sender.stream().nextNotification<McpPrompt.List.Changed.Notification>(McpPrompt.List.Changed)
            .also { notifications.forEach { it() } }
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpPrompt.List,
        McpPrompt.List.Request()
    ).first()
        .nextEvent<List<McpPrompt>, McpPrompt.List.Response> { prompts }.map { it.second }

    override fun get(
        name: PromptName,
        request: PromptRequest,
        overrideDefaultTimeout: Duration?
    ) = sender(McpPrompt.Get, McpPrompt.Get.Request(name, request, request.meta)).first()
        .nextEvent<PromptResponse, McpPrompt.Get.Response> { PromptResponse(messages, description) }
        .map { it.second }
}
