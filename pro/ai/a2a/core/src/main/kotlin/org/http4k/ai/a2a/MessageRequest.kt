/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

import org.http4k.ai.a2a.model.Message
import org.http4k.core.Request

data class MessageRequest(
    val message: Message,
    val http: Request
)
