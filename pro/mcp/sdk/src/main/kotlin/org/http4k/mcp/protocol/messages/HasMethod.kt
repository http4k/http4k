package org.http4k.mcp.protocol.messages

import org.http4k.mcp.protocol.McpRpcMethod

interface HasMethod {
    val Method: McpRpcMethod
}
