package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.TaskMeta

sealed interface ClientMessage {
    interface Request : ClientMessage, McpRequest {
        val task: TaskMeta?
    }
    interface Response : ClientMessage, McpResponse
    interface Notification : ClientMessage, McpNotification
}

