/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.server.protocol.Completions
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

fun completions(vararg capabilities: CompletionCapability): Completions = completions(capabilities.toList())

fun completions(capabilities: Iterable<CompletionCapability>): Completions = object : Completions, Iterable<CompletionCapability> by capabilities {

    override fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        capabilities.find { it.toReference() == mcp.ref }
            ?.complete(mcp, client, http)
            ?: throw McpException(InvalidParams)

    override fun invoke(p1: Reference) = capabilities.find { it.toReference() == p1 }
        ?: throw McpException(InvalidParams)
}

