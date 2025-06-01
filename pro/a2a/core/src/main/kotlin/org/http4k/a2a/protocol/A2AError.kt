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

val TASK_NOT_FOUND = ErrorMessage(-32001, "Task not found")
val TASK_NOT_CANCELABLE = ErrorMessage(-32002, "Task not cancelable")
val PUSH_NOTIFICATION_NOT_SUPPORTED = ErrorMessage(-32003, "Push Notification is not supported")
val UNSUPPORTED_OPERATION = ErrorMessage(-32004, "Unsupported operation")
val CONTENT_TYPE_UNSUPPORTED = ErrorMessage(-32005, "Incompatible content types")
val INVALID_AGENT_RESPONSE = ErrorMessage(-32006, "Invalid agent response")
