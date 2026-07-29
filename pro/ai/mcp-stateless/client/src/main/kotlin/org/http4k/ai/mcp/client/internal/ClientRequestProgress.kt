/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.protocol.messages.McpProgress

internal class ClientRequestProgress(
    private val register: McpCallbackRegistry
) : McpClient.RequestProgress {

    override fun onProgress(fn: (Progress) -> Unit) {
        register.on(McpProgress.Notification::class) { n, _ ->
            fn(Progress(n.params.progressToken, n.params.progress, n.params.total, n.params.description))
        }
    }
}
