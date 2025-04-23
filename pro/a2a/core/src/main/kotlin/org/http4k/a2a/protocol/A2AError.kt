package org.http4k.a2a.protocol

import org.http4k.core.Response
import org.http4k.jsonrpc.ErrorMessage

/**
 * Sealed type encapsulating the known failure modes of A2A clients
 */
sealed interface A2AError {

    /**
     * Standard error returned within the MCP protocol
     */
    data class Protocol(val error: ErrorMessage) : A2AError

    /**
     * HTTP error, most commonly thrown during sending of a request
     */
    data class Http(val response: Response) : A2AError

    /**
     * It's a timeout when waiting for a response to complete
     */
    data object Timeout : A2AError

    /**
     * Unexpected error
     */
    data class Internal(val cause: Exception) : A2AError
}

val ErrorMessage.Companion.NotFound get() = ErrorMessage(-32001, "Task not found")
val ErrorMessage.Companion.NotCancelable get() = ErrorMessage(-32002, "Task not found")
val ErrorMessage.Companion.PushNotificationsNotSupported get() = ErrorMessage(-32003, "Push notifications unsupported")
val ErrorMessage.Companion.UnsupportedOperation get() = ErrorMessage(-32004, "Unsupported Operation")
