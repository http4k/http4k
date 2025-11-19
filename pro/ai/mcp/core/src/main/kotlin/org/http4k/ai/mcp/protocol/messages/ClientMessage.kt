package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.TaskMetadata

sealed interface ClientMessage {
    interface Request : ClientMessage, McpRequest {
        val task: TaskMetadata?
    }
    interface Response : ClientMessage, McpResponse
    interface Notification : ClientMessage, McpNotification
}

