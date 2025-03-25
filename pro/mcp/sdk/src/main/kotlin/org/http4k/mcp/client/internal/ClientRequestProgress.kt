package org.http4k.mcp.client.internal

import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.protocol.messages.McpRpc

internal class ClientRequestProgress(
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.RequestProgress {

    override fun onProgress(fn: (Progress) -> Unit) {
        register(McpProgress, McpCallback(McpProgress.Notification::class) { n, _ ->
            fn(Progress(n.progress, n.total, n.progressToken))
        })
    }
}
