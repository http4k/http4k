package org.http4k.mcp.protocol

sealed interface McpNotification {
    val method: McpRpcMethod
}
