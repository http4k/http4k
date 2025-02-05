package org.http4k.mcp.client.internal

import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.util.McpNodeType

internal fun interface McpRpcSender {
    operator fun invoke(
        method: McpRpc,
        request: ClientMessage.Request,
        isComplete: (McpNodeType) -> Boolean
    ): Result<RequestId>
}
