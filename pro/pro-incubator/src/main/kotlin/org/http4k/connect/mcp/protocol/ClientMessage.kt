package org.http4k.connect.mcp.protocol

sealed interface ClientMessage {
    interface Request : ClientMessage
    interface Response : ClientMessage
    interface Notification : ClientMessage
}

