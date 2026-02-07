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
import org.http4k.ai.mcp.testing.nextNotification
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
        sender.stream().nextNotification<McpTool.List.Changed.Notification>(McpTool.List.Changed)
            .also { notifications.forEach { it() } }

    override fun list(overrideDefaultTimeout: Duration?) =
        sender(McpTool.List, McpTool.List.Request()).first()
            .nextEvent<List<McpTool>, McpTool.List.Response>(fun McpTool.List.Response.() = tools)
            .map { it.second }

    override fun call(
        name: ToolName,
        request: ToolRequest,
        overrideDefaultTimeout: Duration?
    ) = sender(
        McpTool.Call, McpTool.Call.Request(
            name,
            request.mapValues { McpJson.asJsonObject(it.value) }, request.meta
        )
    ).last()
        .nextEvent<ToolResponse, McpTool.Call.Response> { toToolResponseOrError(this) }
        .map { it.second }
        .flatMapFailure { toToolElicitationRequiredOrError(it) }
}
