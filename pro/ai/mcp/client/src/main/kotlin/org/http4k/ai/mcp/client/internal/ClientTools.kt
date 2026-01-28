package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.model.ToolName
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.ToolResponse.Task
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import java.time.Duration
import kotlin.random.Random

internal class ClientTools(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val sender: org.http4k.ai.mcp.client.internal.McpRpcSender,
    private val random: Random,
    private val defaultTimeout: Duration,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Tools {
    override fun onChange(fn: () -> Unit) {
        register(McpTool.List.Changed, McpCallback(McpTool.List.Changed.Notification::class) { _, _ ->
            fn()
        })
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpTool.List, McpTool.List.Request(),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpTool.List.Response>() }
        .map { it.tools }

    override fun call(name: ToolName, request: ToolRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpTool.Call,
            McpTool.Call.Request(
                name,
                request.mapValues { McpJson.asJsonObject(it.value) },
                request.meta
            ),
            overrideDefaultTimeout ?: defaultTimeout,
            McpMessageId.random(random)
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTool.Call.Response>() }
            .map {
                val task = it.task
                when {
                    task != null -> Task(task, it._meta)
                    it.isError == true -> Error(
                        ErrorMessage(
                            -1, it.content?.joinToString()
                                ?: it.structuredContent?.let(McpJson::asFormatString)
                                ?: "<no message"
                        )
                    )

                    else -> Ok(
                        it.content,
                        it.structuredContent?.let(McpJson::convert),
                        it._meta
                    )
                }
            }
}
