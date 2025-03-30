package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.protocol.messages.McpPrompt
import java.time.Duration

class TestingPrompts(private val send: TestMcpSender) : McpClient.Prompts {
    private val notifications = mutableListOf<() -> Unit>()

    override fun onChange(fn: () -> Unit) {
        notifications += fn
    }
//
//        /**
//         * Expected a list changed notification to be received and process it
//         */
//        fun expectNotification() =
//            client.nextNotification<McpPrompt.List.Changed.Notification>(McpPrompt.List.Changed)
//                .also { notifications.forEach { it() } }

    override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpPrompt>> =
        send(
            McpPrompt.List,
            McpPrompt.List.Request()
        ).nextEvent<McpPrompt.List.Response, List<McpPrompt>> { prompts }.map { it.second }

    override fun get(
        name: PromptName,
        request: PromptRequest,
        overrideDefaultTimeout: Duration?
    ): McpResult<PromptResponse> {
        return send(
            McpPrompt.Get,
            McpPrompt.Get.Request(name, request)
        ).nextEvent<McpPrompt.Get.Response, PromptResponse>({
            PromptResponse(messages, description)
        }).map { it.second }
    }
}
