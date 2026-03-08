/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.server.protocol.Completions
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

class ServerCompletions(private val bindings: Iterable<CompletionCapability>) : Completions {

    constructor(vararg bindings: CompletionCapability) : this(bindings.toList())

    override fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        bindings.find { it.toReference() == mcp.ref }
            ?.complete(mcp, client, http)
            ?: throw McpException(InvalidParams)
}
