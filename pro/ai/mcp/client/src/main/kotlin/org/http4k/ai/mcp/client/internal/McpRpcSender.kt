package org.http4k.ai.mcp.client.internal

import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpRpc
import java.time.Duration

internal fun interface McpRpcSender {
    operator fun invoke(
        rpc: McpRpc,
        message: ClientMessage,
        timeout: Duration,
        messageId: McpMessageId
    ): McpResult<McpMessageId>
}
