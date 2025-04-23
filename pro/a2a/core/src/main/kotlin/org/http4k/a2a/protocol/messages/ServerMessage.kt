package org.http4k.a2a.protocol.messages

sealed interface ServerMessage {
    interface Request : ServerMessage, A2ARequest
    interface Response : ServerMessage, A2AResponse
    interface Notification : ServerMessage, A2ANotification
}
