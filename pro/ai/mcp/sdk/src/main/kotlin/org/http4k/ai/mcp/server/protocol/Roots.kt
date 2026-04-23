/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.CompletionStatus
import org.http4k.ai.mcp.model.Root
import org.http4k.ai.mcp.protocol.messages.McpRoot

/**
 * Handles protocol traffic for client provided roots.
 */
interface Roots : Iterable<Root> {
    fun update(req: McpRoot.List.Response.Result): CompletionStatus
}
