package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.protocol.McpRpcMethod

interface McpRpc {
    val Method: McpRpcMethod
}
