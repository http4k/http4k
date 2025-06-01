package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable
import org.http4k.a2a.protocol.messages.Metadata

@JsonSerializable
data class TaskIdParams(
    val id: TaskId,
    val metadata: Metadata = emptyMap()
)
