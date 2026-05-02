/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

import org.http4k.ai.a2a.model.StreamMessage

sealed interface MessageResponse {
    data class Task(val task: org.http4k.ai.a2a.model.Task) : MessageResponse

    data class Message(val message: org.http4k.ai.a2a.model.Message) : MessageResponse

    data class Stream(val responses: Sequence<StreamMessage>) : MessageResponse
}
