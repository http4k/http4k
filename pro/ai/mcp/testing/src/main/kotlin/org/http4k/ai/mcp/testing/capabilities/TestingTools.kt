/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.internal.toToolElicitationRequiredOrError
import org.http4k.ai.mcp.client.internal.toToolResponseOrError
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.toNotification
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.model.ToolName
import java.time.Duration

class TestingTools(
    private val sender: TestMcpSender
) : McpClient.Tools {
    private val notifications = mutableListOf<() -> Unit>()

    override fun onChange(fn: () -> Unit) {
        notifications += fn
    }

    /**
     * Force a list changed notification to be received and process it
     */
    fun expectNotification() =
        sender.lastEvent()
            .toNotification<McpTool.List.Changed.Notification.Params>(McpTool.List.Changed.Method)
            .also { notifications.forEach { it() } }

    override fun list(overrideDefaultTimeout: Duration?) =
        sender(McpTool.List.Request(McpTool.List.Request.Params(), sender.nextId())).first()
            .nextEvent<List<McpTool>, McpTool.List.Response.Result>(fun McpTool.List.Response.Result.() = tools)
            .map { it.second }

    override fun call(
        name: ToolName,
        request: ToolRequest,
        overrideDefaultTimeout: Duration?
    ) = sender(
        McpTool.Call.Request(
            McpTool.Call.Request.Params(
                name,
                request.mapValues { McpJson.asJsonObject(it.value) }, request.meta
            ), sender.nextId()
        )
    ).last()
        .nextEvent<ToolResponse, McpTool.Call.Response.Result> { toToolResponseOrError(this) }
        .map { it.second }
        .flatMapFailure { toToolElicitationRequiredOrError(it) }
}
