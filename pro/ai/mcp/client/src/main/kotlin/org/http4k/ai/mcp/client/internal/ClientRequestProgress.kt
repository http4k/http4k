/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpRpc

internal class ClientRequestProgress(
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.RequestProgress {

    override fun onProgress(fn: (Progress) -> Unit) {
        register(McpProgress, McpCallback(McpProgress.Notification.Params::class) { n, _ ->
            fn(Progress(n.progressToken, n.progress, n.total, n.description))
        })
    }
}
