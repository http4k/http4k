/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.SamplingHandler
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse.Error
import org.http4k.ai.mcp.SamplingResponse.Ok
import org.http4k.ai.mcp.SamplingResponse.Task
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.lens.MetaKey
import org.http4k.lens.progressToken
import java.time.Duration

class TestingSampling(sender: TestMcpSender) : McpClient.Sampling {

    private val onSampling = mutableListOf<SamplingHandler>()

    override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {
        onSampling.add(fn)
    }

    init {
        sender.on(McpSampling) { event ->
            val (id, req) =
                event.nextEvent<SamplingRequest, McpSampling.Request> {
                    SamplingRequest(
                        messages, maxTokens,
                        systemPrompt, includeContext,
                        temperature, stopSequences,
                        modelPreferences, metadata,
                        tools ?: emptyList(),
                        toolChoice,
                        MetaKey.progressToken<Any>().toLens()(_meta),
                        )
                }.valueOrNull()!!
            onSampling.forEach { handler ->
                handler(req).forEach { response ->
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
                    sender(protocolResponse, id!!)
                }
            }
        }
    }
}
