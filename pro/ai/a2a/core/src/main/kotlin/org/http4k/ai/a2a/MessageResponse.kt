/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

sealed interface MessageResponse {
    data class Task(val tasks: Sequence<org.http4k.ai.a2a.model.Task>) : MessageResponse {
        constructor(task: org.http4k.ai.a2a.model.Task) : this(sequenceOf(task))
    }

    data class Message(val message: org.http4k.ai.a2a.model.Message) : MessageResponse
}
