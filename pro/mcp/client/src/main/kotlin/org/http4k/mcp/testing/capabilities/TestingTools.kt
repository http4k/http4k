package org.http4k.mcp.testing.capabilities

import dev.forkhandles.result4k.map
import org.http4k.connect.model.ToolName
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.Content
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.testing.TestMcpSender
import org.http4k.mcp.testing.nextEvent
import org.http4k.mcp.testing.nextNotification
import org.http4k.mcp.util.McpJson
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
        .nextEvent<ToolResponse, McpTool.Call.Response> {
            when (isError) {
                true -> {
                    val input = (content?.first() as Content.Text).text
                    ToolResponse.Error(McpJson.asA<ErrorMessage>(input))
                }

                else -> Ok(
                    content,
                    structuredContent?.let(McpJson::convert),
                    _meta
                )

            }
        }.map { it.second }
}
