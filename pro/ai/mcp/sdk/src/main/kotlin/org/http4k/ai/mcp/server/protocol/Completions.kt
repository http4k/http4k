/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.core.Request

interface Completions : (Reference) -> CompletionHandler {
    fun complete(mcp: McpCompletion.Request, client: Client, http: Request): McpCompletion.Response
}
