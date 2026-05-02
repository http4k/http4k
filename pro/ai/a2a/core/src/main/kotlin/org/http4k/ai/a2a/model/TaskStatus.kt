/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

import java.time.Instant

data class TaskStatus(
    val state: TaskState,
    val message: Message? = null,
    val timestamp: Instant? = null
)
