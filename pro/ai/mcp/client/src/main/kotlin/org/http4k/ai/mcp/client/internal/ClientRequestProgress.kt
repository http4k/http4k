package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpRpc

internal class ClientRequestProgress(
    private val register: (McpRpc, McpCallback<*>) -> Any
) : McpClient.RequestProgress {

    override fun onProgress(fn: (Progress) -> Unit) {
        register(McpProgress, McpCallback(McpProgress.Notification::class) { n, _ ->
            fn(Progress(n.progressToken, n.progress, n.total, n.description))
        })
    }
}
