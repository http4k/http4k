package org.http4k.ai.a2a

import org.http4k.core.Response
import org.http4k.jsonrpc.ErrorMessage

sealed interface A2AError {
    data class Protocol(val error: ErrorMessage) : A2AError
    data class Http(val response: Response) : A2AError
    data object Timeout : A2AError
    data class Internal(val cause: Exception) : A2AError
}
