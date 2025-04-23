package org.http4k.a2a.protocol.messages

sealed interface ClientMessage {
    interface Request : ClientMessage, A2ARequest, HasMetadata
    interface Response : ClientMessage, A2AResponse
    interface Notification : ClientMessage, A2ANotification
}
