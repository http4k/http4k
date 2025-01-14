package org.http4k.connect.mcp

sealed interface ClientMessage {
    interface Request : ClientMessage
    interface Response : ClientMessage
    interface Notification : ClientMessage
}

