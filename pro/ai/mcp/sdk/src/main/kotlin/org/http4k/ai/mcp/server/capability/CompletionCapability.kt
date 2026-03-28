/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.CompletionFilter
import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse.Error
import org.http4k.ai.mcp.CompletionResponse.Ok
import org.http4k.ai.mcp.model.Completion
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Reference.Prompt
import org.http4k.ai.mcp.model.Reference.ResourceTemplate
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.then
import org.http4k.core.Request

class CompletionCapability(
    internal val ref: Reference,
    internal val handler: CompletionHandler
) : ServerCapability, CompletionHandler {

    fun toReference() = ref

    fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        when (val result = handler(CompletionRequest(mcp.argument, mcp.context, mcp._meta, client, http))) {
            is Ok -> McpCompletion.Response(Completion(result.values, result.total, result.hasMore))
            is Error -> throw McpException(result.error)
        }

    override fun invoke(p1: CompletionRequest) = handler(p1)

    override val name = when (ref) {
        is Prompt -> ref.name
        is ResourceTemplate -> ref.uri.toString()
    }
}

fun CompletionFilter.then(capability: CompletionCapability) = CompletionCapability(capability.ref, then(capability))
