/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import org.http4k.ai.a2a.protocol.messages.SendMessageConfiguration
import org.http4k.core.Request

data class MessageRequest(
    val message: Message,
    val configuration: SendMessageConfiguration? = null,
    val metadata: Map<String, Any>? = null,
    val http: Request
)
