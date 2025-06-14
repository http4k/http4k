package org.http4k.ai.llm

import org.http4k.core.Response

sealed interface LLMError {
    data class Http(val response: Response) : LLMError

    data object Timeout : LLMError

    data object NotFound : LLMError

    data class Internal(val cause: Exception) : LLMError

    data class Custom<T>(val error: T) : LLMError
}
