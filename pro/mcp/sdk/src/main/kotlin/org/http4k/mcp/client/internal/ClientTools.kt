package org.http4k.mcp.client.internal

import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage

internal class ClientTools(
    private val queueFor: (RequestId) -> Iterable<SseMessage.Event>,
    private val sender: McpRpcSender,
    private val register: (NotificationCallback<*>) -> Any
) : McpClient.Tools {
    override fun onChange(fn: () -> Unit) {
        register(
            NotificationCallback(
                McpTool.List.Changed,
                McpTool.List.Changed.Notification::class
            ) { fn() }
        )
    }

    override fun list() = sender(McpTool.List, McpTool.List.Request()) { true }
        .mapCatching(queueFor).map { it.first().asAOrThrow<McpTool.List.Response>() }
        .map { it.tools }

    override fun call(name: ToolName, request: ToolRequest) =
        sender(
            McpTool.Call,
            McpTool.Call.Request(name, request.mapValues { McpJson.asJsonObject(it.value) })
        ) { true }
            .mapCatching(queueFor).map { it.first().asAOrThrow<McpTool.Call.Response>() }
            .mapCatching {
                when (it.isError) {
                    true -> ToolResponse.Error(ErrorMessage(-1, it.content.joinToString()))
                    else -> ToolResponse.Ok(it.content)
                }
            }
}
