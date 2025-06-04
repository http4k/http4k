package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.messages.McpCancelled

/**
 * Handles Cancellation requests
 */
interface Cancellations {
    fun onCancel(callback: CancellationCallback)
    fun cancel(cancellation: McpCancelled.Notification)
}

fun interface CancellationCallback {
    operator fun invoke(id: McpMessageId, reason: String?, meta: Meta)
}
