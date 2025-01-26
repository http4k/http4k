package org.http4k.mcp.protocol.messages

import org.http4k.mcp.protocol.McpRpcMethod

sealed interface McpNotification {
    val method: McpRpcMethod
}
