package org.http4k.a2a.protocol.model

import org.http4k.a2a.protocol.messages.Metadata
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Task(
    val id: TaskId,
    val contextId: ContextId,
    val status: TaskStatus,
    val history: List<Message>? = null,
    val artifacts: List<Artifact>? = null,
    val metadata: Metadata = emptyMap()
)
