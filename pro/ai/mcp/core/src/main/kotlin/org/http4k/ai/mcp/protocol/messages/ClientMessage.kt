package org.http4k.ai.mcp.protocol.messages

sealed interface ClientMessage {
    interface Request : ClientMessage, McpRequest
    interface Response : ClientMessage, McpResponse
    interface Notification : ClientMessage, McpNotification
}

