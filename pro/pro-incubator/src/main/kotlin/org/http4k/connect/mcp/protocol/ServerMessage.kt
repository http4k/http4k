package org.http4k.connect.mcp.protocol

sealed interface ServerMessage {
    interface Request : ServerMessage
    interface Response : ServerMessage {
        object Empty : Response
    }
    interface Notification : ServerMessage {
        val method: McpRpcMethod
    }
}

