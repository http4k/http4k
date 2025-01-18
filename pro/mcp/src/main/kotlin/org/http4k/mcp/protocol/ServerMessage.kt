package org.http4k.mcp.protocol

sealed interface ServerMessage {
    interface Request : ServerMessage, McpRequest
    interface Response : ServerMessage, McpResponse {
        object Empty : Response
    }

    interface Notification : ServerMessage, McpNotification {
        val method: McpRpcMethod
    }
}

