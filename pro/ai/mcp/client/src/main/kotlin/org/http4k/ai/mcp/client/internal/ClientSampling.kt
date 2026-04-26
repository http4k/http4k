/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.SamplingHandler
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse.Error
import org.http4k.ai.mcp.SamplingResponse.Ok
import org.http4k.ai.mcp.SamplingResponse.Task
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpSampling
import java.time.Duration

internal class ClientSampling(
    private val tidyUp: (McpMessageId) -> Unit,
    private val defaultTimeout: Duration,
    private val sender: McpRpcSender,
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.Sampling {

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        register(McpSampling,
            McpCallback(McpSampling.Request::class) { request, requestId ->
                if (requestId == null) return@McpCallback

                val responses = fn(
                    SamplingRequest(
                        request.messages,
                        request.maxTokens,
                        request.systemPrompt,
                        request.includeContext,
                        request.temperature,
                        request.stopSequences,
                        request.modelPreferences,
                        request.metadata,
                        request.tools ?: emptyList(),
                        request.toolChoice
                    )
                )

                val timeout = overrideDefaultTimeout ?: defaultTimeout

                responses.forEach { response ->
                    val protocolResponse = when (response) {
                        is Ok -> McpSampling.Response(
                            response.model,
                            response.stopReason,
                            response.role,
                            response.content
                        )

                        is Task -> McpSampling.Response(task = response.task)
                        is Error -> throw McpException(DomainError(response.message))
                    }
                    sender(
                        McpSampling,
                        protocolResponse,
                        timeout,
                        requestId
                    )

                    when (response) {
                        is Ok -> if (response.stopReason != null) tidyUp(requestId)
                        else -> tidyUp(requestId)
                    }
                }
            })
    }
}
