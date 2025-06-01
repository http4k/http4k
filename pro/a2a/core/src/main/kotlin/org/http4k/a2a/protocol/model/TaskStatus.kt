package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class TaskStatus(
    val state: TaskState,
    val message: Message? = null,
    val timestamp: Instant? = null
)
