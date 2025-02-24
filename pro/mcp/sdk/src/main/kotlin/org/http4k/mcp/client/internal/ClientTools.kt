package org.http4k.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import java.time.Duration

internal class ClientTools(
    private val queueFor: (RequestId) -> Iterable<McpNodeType>,
    private val tidyUp: (RequestId) -> Unit,
    private val sender: McpRpcSender,
    private val defaultTimeout: Duration,
    private val register: (McpRpc, NotificationCallback<*>) -> Any
) : McpClient.Tools {
    override fun onChange(fn: () -> Unit) {
        register(McpTool.List.Changed, NotificationCallback(McpTool.List.Changed.Notification::class) { fn() })
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpTool.List, McpTool.List.Request(),
        overrideDefaultTimeout ?: defaultTimeout
    ) { true }
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpTool.List.Response>() }
        .map { it.tools }

    override fun call(name: ToolName, request: ToolRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpTool.Call,
            McpTool.Call.Request(name, request.mapValues { McpJson.asJsonObject(it.value) }),
            overrideDefaultTimeout ?: defaultTimeout
        ) { true }
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTool.Call.Response>() }
            .map {
                when (it.isError) {
                    true -> Error(ErrorMessage(-1, it.content.joinToString()))
                    else -> Ok(it.content)
                }
            }
}
