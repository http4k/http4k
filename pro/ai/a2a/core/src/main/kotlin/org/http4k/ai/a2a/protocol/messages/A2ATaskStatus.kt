/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.TaskState
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class A2ATaskStatus(
    val state: TaskState,
    val message: A2AMessage? = null,
    val timestamp: Instant? = null
)
