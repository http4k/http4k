package org.http4k.ai

import org.http4k.core.Response

sealed interface AiError {
    data class Http(val response: Response) : AiError

    data object Timeout : AiError

    data class Internal(val cause: Exception) : AiError
}
