/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.internal.toPromptErrorOrFailure
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.toNotification
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
        sender.lastEvent()
            .toNotification<McpPrompt.List.Changed.Notification.Params>(McpPrompt.List.Changed.Method)
            .also { notifications.forEach { it() } }
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpPrompt.List.Request(McpPrompt.List.Request.Params(), sender.nextId())
    ).first()
        .nextEvent<List<McpPrompt>, McpPrompt.List.Response.Result> { prompts }.map { it.second }

    override fun get(
        name: PromptName,
        request: PromptRequest,
        overrideDefaultTimeout: Duration?
    ) = sender(McpPrompt.Get.Request(McpPrompt.Get.Request.Params(name, request, request.meta), sender.nextId())).first()
        .nextEvent<PromptResponse, McpPrompt.Get.Response.Result> { PromptResponse.Ok(messages, description) }
        .map { it.second }
        .flatMapFailure { toPromptErrorOrFailure(it) }
}
