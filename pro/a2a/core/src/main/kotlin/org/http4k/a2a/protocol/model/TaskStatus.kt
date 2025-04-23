package org.http4k.a2a.protocol.model

import java.time.Instant

data class TaskStatus(
    val state: TaskState,
    val message: Message? = null,
    val timestamp: Instant? = null
)
