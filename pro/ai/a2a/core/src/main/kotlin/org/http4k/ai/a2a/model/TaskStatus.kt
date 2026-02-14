package org.http4k.ai.a2a.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class TaskStatus(
    val state: TaskState,
    val message: Message? = null,
    val timestamp: Instant? = null
)
