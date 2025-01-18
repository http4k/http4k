package org.http4k.mcp.protocol

sealed interface ClientMessage {
    interface Request : ClientMessage
    interface Response : ClientMessage
    interface Notification : ClientMessage
}

