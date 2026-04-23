/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.PromptResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.util.McpNodeType
import java.time.Duration

internal class ClientPrompts(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val id: () -> McpMessageId,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Prompts {
    override fun onChange(fn: () -> Unit) {
        register(McpPrompt.List, McpCallback(McpPrompt.List.Changed.Notification.Params::class) { _, _ ->
            fn()
        })
    }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpPrompt.List,
        McpPrompt.List.Request.Params(), overrideDefaultTimeout ?: defaultTimeout, id()
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpPrompt.List.Response.Result>() }
        .map { it.prompts }

    override fun get(name: PromptName, request: PromptRequest, overrideDefaultTimeout: Duration?) =
        sender(
            McpPrompt.Get,
            McpPrompt.Get.Request.Params(name, request, request.meta),
            overrideDefaultTimeout ?: defaultTimeout,
            id()
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpPrompt.Get.Response.Result>() }
            .map { Ok(it.messages, it.description) as PromptResponse }
            .flatMapFailure { toPromptErrorOrFailure(it) }
}
