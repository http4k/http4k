package org.http4k.mcp.client.internal

import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.ClientMessage.Request
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpNodeType
import java.time.Duration

internal fun interface McpRpcSender {
    operator fun invoke(
        rpc: McpRpc,
        request: Request,
        timeout: Duration,
        isComplete: (McpNodeType) -> Boolean
    ): McpResult<RequestId>
}
