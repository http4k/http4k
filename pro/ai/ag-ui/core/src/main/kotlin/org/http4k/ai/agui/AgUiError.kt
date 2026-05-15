/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui

import org.http4k.core.Response

sealed interface AgUiError {
    data class Http(val response: Response) : AgUiError
}
