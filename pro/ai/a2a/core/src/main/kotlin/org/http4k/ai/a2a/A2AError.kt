/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

import org.http4k.core.Response
import org.http4k.jsonrpc.ErrorMessage

sealed interface A2AError {
    data class Protocol(val error: ErrorMessage) : A2AError
    data class Http(val response: Response) : A2AError
    data object Timeout : A2AError
    data class Internal(val cause: Exception) : A2AError
}
