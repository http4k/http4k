/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.CompletionStatus
import org.http4k.ai.mcp.model.CompletionStatus.Finished
import org.http4k.ai.mcp.model.Root
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.server.protocol.Roots
import org.http4k.ai.mcp.util.ObservableList
import org.http4k.core.Request

fun roots(): Roots = InMemoryRoots()

private class InMemoryRoots : ObservableList<Root>(emptyList()), Roots {

    override fun changed(params: McpRoot.Changed.Notification.Params, client: Client, http: Request) =
        client.requestRoots(params._meta)

    override fun update(req: McpRoot.List.Response.Result): CompletionStatus {
        items = req.roots
        return Finished
    }
}
