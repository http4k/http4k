/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.ai.model.ToolName
import java.time.Duration

internal class ClientTools(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val sender: McpRpcSender,
    private val id: () -> McpMessageId,
    private val defaultTimeout: Duration,
    private val register: McpCallbackRegistry
) : McpClient.Tools {

    override fun onChange(fn: () -> Unit) {
        register.on(McpTool.List.Changed.Notification::class) { _, _ -> fn() }
    }

    override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpTool>> {
        val messageId = id()
        return sender(
            McpTool.List.Request(McpTool.List.Request.Params(), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTool.List.Response.Result>() }
            .map { it.tools }
    }

    override fun call(name: ToolName, request: ToolRequest, overrideDefaultTimeout: Duration?): McpResult<ToolResponse> {
        val messageId = id()
        return sender(
            McpTool.Call.Request(
                McpTool.Call.Request.Params(
                    name,
                    request.mapValues { McpJson.asJsonObject(it.value) },
                    request.meta
                ),
                messageId
            ),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTool.Call.Response.Result>() }
            .map { toToolResponseOrError(it) }
            .flatMapFailure { toToolElicitationRequiredOrError(it) }
    }
}
